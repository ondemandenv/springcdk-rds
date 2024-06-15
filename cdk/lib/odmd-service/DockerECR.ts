import {RemovalPolicy, SecretValue, Stack} from "aws-cdk-lib";
import {
    BuildSpec,
    Cache,
    ComputeType,
    LinuxBuildImage,
    LocalCacheMode,
    PipelineProject
} from "aws-cdk-lib/aws-codebuild";
import * as ecr from "aws-cdk-lib/aws-ecr";
import {Grant, Role} from "aws-cdk-lib/aws-iam";
import * as codepipeline from "aws-cdk-lib/aws-codepipeline";
import {Pipeline} from "aws-cdk-lib/aws-codepipeline";
import {
    CodeBuildAction,
    CodeCommitSourceAction,
    GitHubSourceAction,
    GitHubTrigger
} from "aws-cdk-lib/aws-codepipeline-actions";
import {IRepository, Repository} from "aws-cdk-lib/aws-codecommit";
import {IVpc} from "aws-cdk-lib/aws-ec2";
import {BuildEnvironmentVariable} from "aws-cdk-lib/aws-codebuild/lib/project";
import {Bucket, BucketEncryption} from "aws-cdk-lib/aws-s3";
import {Construct} from "constructs";
import {LogGroup} from "aws-cdk-lib/aws-logs";
import {ContractsEnverCdk, OndemandContracts} from "@ondemandenv/odmd-contracts";
import {Secret} from "aws-cdk-lib/aws-secretsmanager";
import {StringParameter} from "aws-cdk-lib/aws-ssm";
import {AwsCustomResource, AwsCustomResourcePolicy, PhysicalResourceId} from "aws-cdk-lib/custom-resources";


type ImgTagEcr = { builtImgName: string, builtImgTag: string, ecrImgTags?: string[] };

export interface DockerECRConfig {
    readonly workdirs?: string[]
    readonly dockerBuildCmd?: string;
    readonly imageTaggings: ImgTagEcr[]
    readonly srcRepo?: IRepository
    readonly vpc?: IVpc,
}

export class DockerECR<T extends ContractsEnverCdk> extends Construct {
    public static readonly LATEST_IMG_TAG = 'latest';
    public readonly buildImgProj: PipelineProject;
    public readonly buildImgPipeline: Pipeline;

    public readonly builtImgToRepo: Map<string, ecr.Repository> = new Map<string, ecr.Repository>()

    //todo: https://aws.amazon.com/blogs/devops/reducing-docker-image-build-time-on-aws-codebuild-using-an-external-cache/
    constructor(owner: Stack, imgConfig: DockerECRConfig, enver: T) {
        super(owner, enver.owner.buildId + '-dockerEcr');

        if (!imgConfig.dockerBuildCmd) {
            throw new Error('need build command')
        }
        if (!imgConfig.imageTaggings || imgConfig.imageTaggings.length == 0) {
            throw new Error('need at least one image tag configured')
        }

        const buildCmds = []
        if (imgConfig.workdirs) {
            buildCmds.push('cd ' + imgConfig.workdirs.join('/'))
        }
        buildCmds.push(
            'echo $CODEBUILD_RESOLVED_SOURCE_VERSION',
            `${imgConfig.dockerBuildCmd}`,
        )
        const environmentVariables: { [name: string]: BuildEnvironmentVariable } = {};

        imgConfig.imageTaggings.reduce((p, c) => {
            if (!p.has(c.builtImgName))
                p.set(c.builtImgName, new Array<ImgTagEcr>())
            p.get(c.builtImgName)!.push(c)
            return p
        }, new Map<string, ImgTagEcr[]>).forEach((tags, builtImgId) => {
            const builtId = DockerECR.sanitizeVarName(builtImgId)
            buildCmds.push('echo $ECR_REPO_URI_' + builtId)
            tags.forEach((imgTagging) => {
                let {builtImgName, builtImgTag, ecrImgTags} = imgTagging
                if (!ecrImgTags) ecrImgTags = []
                ecrImgTags.push(DockerECR.LATEST_IMG_TAG)
                ecrImgTags.push('$CODEBUILD_RESOLVED_SOURCE_VERSION')

                ecrImgTags.forEach(t => {
                    buildCmds.push(
                        `docker tag ${builtImgName}:${builtImgTag} $ECR_REPO_URI_${builtId}:${t}`,
                        `docker image ls`
                    )
                })

                buildCmds.push(
                    `aws ecr get-login-password --region ${owner.region} | docker login --username AWS --password-stdin ${owner.account}.dkr.ecr.${owner.region}.amazonaws.com`,
                    `docker push --all-tags $ECR_REPO_URI_${builtId}`)

                const repository = new ecr.Repository(owner, enver.node.id + '-' + builtImgName + '-repo', {
                    removalPolicy: enver.ephemeral ? RemovalPolicy.DESTROY : RemovalPolicy.SNAPSHOT
                });
                this.builtImgToRepo.set(builtImgName, repository)
                environmentVariables[DockerECR.sanitizeVarName(`ECR_REPO_URI_${builtId}`)] = {value: repository.repositoryUriForTag()}
            })
        })

        const buildLog = new LogGroup(owner, enver.owner.buildId + '_build_img_proj_log', {
            removalPolicy: enver.ephemeral ? RemovalPolicy.DESTROY : RemovalPolicy.RETAIN,
            // logGroupName: enver.owner.buildId + '_' + OndemandContracts.REV_REF_value.substring(2)
        })

        const srcRole = imgConfig.srcRepo ? undefined : Role.fromRoleArn(owner, 'infra-repo-role', enver.centralRepoSrcRoleArn, {mutable: false}) as Role;
        const buildRole = imgConfig.srcRepo ? undefined : Role.fromRoleArn(owner, `build-role`, enver.buildRoleArn, {mutable: false}) as Role;

        this.buildImgProj = new PipelineProject(owner, enver.node.id + '_build_img_proj', {
            // role: buildRole,
            buildSpec: BuildSpec.fromObject({
                version: '0.2',
                phases: {
                    install: {
                        'runtime-versions': {
                            docker: 20,
                            java: 'corretto17'
                        }
                    },
                    build: {
                        commands: buildCmds
                    }
                }
            }),
            logging: {
                cloudWatch: {logGroup: buildLog}
            },
            cache: Cache.local(LocalCacheMode.DOCKER_LAYER),
            environment: {
                buildImage: LinuxBuildImage.AMAZON_LINUX_2_4,
                computeType: ComputeType.LARGE,
                privileged: true
            },
            environmentVariables
        });
        this.builtImgToRepo.forEach(v => {
            v.grantPullPush(this.buildImgProj)
        })

        /*Service role arn:aws:iam::420887418376:role/spring-rds-cdk-odmdSbxUsw-lebuildimgprojRole5AB5080-GYKBoZAnPeiH does not allow AWS CodeBuild to create Amazon CloudWatch Logs log streams for build arn:aws:codebuild:us-west-1:420887418376:build/lebuildimgproj6B15E5F1-EmuRLDmzGOUH:87c7b68a-3ad4-40b2-b799-1e81c2ef36d3. Error message: User: arn:aws:sts::420887418376:assumed-role/spring-rds-cdk-odmdSbxUsw-lebuildimgprojRole5AB5080-GYKBoZAnPeiH/AWSCodeBuild-87c7b68a-3ad4-40b2-b799-1e81c2ef36d3 is not authorized to perform: logs:CreateLogStream on resource: arn:aws:logs:us-west-1:420887418376:log-group:/aws/codebuild/lebuildimgproj6B15E5F1-EmuRLDmzGOUH:log-stream:87c7b68a-3ad4-40b2-b799-1e81c2ef36d3 because no identity-based policy allows the logs:CreateLogStream action*/
        Grant.addToPrincipal({
            grantee: this.buildImgProj,
            actions: ['logs:CreateLogStream', 'logs:PutLogEvents'],
            resourceArns: [buildLog.logGroupArn, buildLog.logGroupArn + '*'],
            scope: buildLog
        });

        const output: codepipeline.Artifact = new codepipeline.Artifact()

        /*
        //dockerEcr/CopyValueResource/Resource/Default (springrdscdkdockerEcrCopyValueResource4691126C) SSM Secure reference is not supported in: [Custom::AWS/Properties/Create,Custom::AWS/Properties/Update]
                const ssmParameter = StringParameter.fromSecureStringParameterAttributes(this, 'repoReadToken-ssm', {
                    parameterName: `/gyang-tst/${enver.owner.buildId}/${enver.targetRevision.value}/repoReadToken`
                });
        */

        const ssmParameter = StringParameter.fromStringParameterName(this, 'repoReadToken-ssm',
            `/gyang-tst/${enver.owner.buildId}/${enver.targetRevision.value}/repoReadToken`);

        const secret = new Secret(this, 'repoReadToken-secret');

        const copyValueResource = new AwsCustomResource(this, 'CopyValueResource', {
            onUpdate: {
                service: 'SecretsManager',
                action: 'putSecretValue',
                parameters: {
                    SecretId: secret.secretArn,
                    SecretString: ssmParameter.stringValue,
                },
                physicalResourceId: PhysicalResourceId.of('CopyValueResource'),
            },
            policy: AwsCustomResourcePolicy.fromSdkCalls({
                resources: AwsCustomResourcePolicy.ANY_RESOURCE,
            }),
        });

        ssmParameter.grantRead(copyValueResource);

        const codeCommitSourceAction =
            new GitHubSourceAction({
                actionName: 'img_src',
                owner: enver.owner.gitHubRepo.owner,
                repo: enver.owner.gitHubRepo.repo,
                branch: OndemandContracts.REV_REF_value.substring(2),
                trigger: GitHubTrigger.POLL,
                oauthToken: SecretValue.secretsManager(secret.secretArn),
                output
            })


        this.buildImgPipeline = new Pipeline(owner, enver.node.id + '_build_img_pipeline', {
            pipelineName: enver.owner.buildId + OndemandContracts.REV_REF_value.substring(2) + '_build_imgs',
            role: buildRole,
            stages: [
                {
                    stageName: 'Source',
                    actions: [
                        codeCommitSourceAction,
                    ],
                },
                {
                    stageName: 'Build',
                    actions: [
                        new CodeBuildAction({
                            actionName: 'img_build_ecr',
                            project: this.buildImgProj,
                            input: output
                        })
                    ]
                }
            ],
            artifactBucket: new Bucket(owner, enver.node.id + '_build_img_pipeline_artifact', {
                encryption: BucketEncryption.KMS
            })
        });

    }

    static sanitizeVarName(o: string, len: number = -1): string {
        const rt = o.replace(new RegExp(/[^a-zA-Z0-9_]/, 'g'), '');
        if (len < 0) {
            return rt
        }
        return rt.length > len ? rt.substring(0, len) : rt
    }
}
