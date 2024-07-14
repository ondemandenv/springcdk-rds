import {RemovalPolicy, Stack} from "aws-cdk-lib";
import {IVpc, SecurityGroup} from "aws-cdk-lib/aws-ec2";
import {AccountPrincipal, Grant, PolicyStatement, Role} from "aws-cdk-lib/aws-iam";
import {LogGroup} from "aws-cdk-lib/aws-logs";
import * as pipeline from "aws-cdk-lib/aws-codepipeline";
import {Pipeline} from "aws-cdk-lib/aws-codepipeline";
import * as ecr from "aws-cdk-lib/aws-ecr";
import * as ecs from "aws-cdk-lib/aws-ecs";
import {ContainerImage, FargatePlatformVersion, TaskDefinition} from "aws-cdk-lib/aws-ecs";
import {
    BuildEnvironmentVariableType,
    BuildSpec,
    ComputeType,
    LinuxBuildImage,
    PipelineProject
} from "aws-cdk-lib/aws-codebuild";
import {CodeBuildAction, EcrSourceAction, EcsDeployAction} from "aws-cdk-lib/aws-codepipeline-actions";
import {ApplicationLoadBalancedFargateService} from "aws-cdk-lib/aws-ecs-patterns";
import * as fs from "fs";
import {Base64} from "js-base64";
import {Bucket} from "aws-cdk-lib/aws-s3";
import {Construct} from "constructs";
import {ContractsEnverCdk, OndemandContracts} from "@ondemandenv/odmd-contracts";

export interface EcrECSConfig {
    readonly vpc: IVpc

    readonly appImgRepo: ecr.IRepository
    readonly appImgTag: string
    readonly appContainerName?: string
    readonly targetService?: ecs.BaseService

    readonly migImgRepo: ecr.IRepository
    readonly migImgTag: string
    readonly migContainerName?: string
    readonly migTaskSGs: SecurityGroup[]

    readonly migTaskDef: TaskDefinition

}

export class EcrECS<T extends ContractsEnverCdk> extends Construct {
    public static readonly PLACE_HOLDER_IMG = 'public.ecr.aws/ecs-sample-image/amazon-ecs-sample:latest';

    readonly deployImgPipeline: Pipeline
    readonly ecsDeployAction: EcsDeployAction
    readonly targetService: ecs.BaseService

    constructor(stack: Stack, ecsConfig: EcrECSConfig, appConfig: T) {
        super(stack, appConfig.owner.buildId + '-ecr-ecs')
        const appContainerName = ecsConfig.appContainerName ?? 'default'
        if (ecsConfig.targetService && !ecsConfig.targetService.taskDefinition.findContainer(appContainerName)) {
            throw new Error(`target service find no container to be deployed, container name:${appContainerName}`)
        }
        const migTaskSubnets = ecsConfig.vpc.privateSubnets.length > 0 ? ecsConfig.vpc.privateSubnets : ecsConfig.vpc.isolatedSubnets;
        if (migTaskSubnets.length == 0) {
            throw new Error(`Can't find subnets to put task in`)
        }
        if (ecsConfig.migTaskSGs.length == 0) {
            throw new Error(`Can't find migTaskSGs`)
        }

        const appImgFromECR = new pipeline.Artifact();
        const migImgFromECR = new pipeline.Artifact();

        const imgFromEcrDef = new pipeline.Artifact();

        const taskdefImgFileName = "imagedefinitions.json";

        const appImgDef = Base64.encode(fs.readFileSync(__dirname + "/ecrs-utils/genAppContainerImgDef.ts", 'utf-8'));
        const runMigTsk = Base64.encode(fs.readFileSync(__dirname + "/ecrs-utils/deployMigrateTask.ts", 'utf-8'));

        const buildDefLog = new LogGroup(stack, appConfig.owner.buildId + '_buildDefLog', {
            removalPolicy: appConfig.ephemeral ? RemovalPolicy.DESTROY : RemovalPolicy.SNAPSHOT
        });
        const buildImgDefProj = new PipelineProject(stack, appConfig.owner.buildId + 'build_img_def_', {
            buildSpec: BuildSpec.fromObject({
                version: '0.2',
                phases: {
                    "install": {
                        "runtime-versions": {"nodejs": 16},
                        "commands": [
                            "npm install -g typescript",
                            "npm install -g ts-node",
                            "npm install -g aws-sdk@2.1274.0",
                            "npm install -g cross-env",
                            `echo '${appImgDef}' | base64 -d > index.js`
                        ]
                    },
                    build: {
                        commands: [
                            `ls -ltarh $CODEBUILD_SRC_DIR/`,
                            `ecrImgDetail=$CODEBUILD_SRC_DIR/imageDetail.json ecsContainerName=${appContainerName} output=${taskdefImgFileName} ts-node index.js`,
                            `cat ${taskdefImgFileName}`
                        ]
                    }
                },
                artifacts: {
                    files: [
                        taskdefImgFileName
                    ]
                }
            }),
            environment: {
                buildImage: LinuxBuildImage.AMAZON_LINUX_2_4,
                computeType: ComputeType.MEDIUM
            },
            environmentVariables: {
                ecrUri: {type: BuildEnvironmentVariableType.PLAINTEXT, value: ecsConfig.appImgRepo.repositoryUri}
            },
            logging: {
                cloudWatch: {logGroup: buildDefLog}
            },
        });

        if (!ecsConfig) {
            this.appLbFargateService = this.createEcsService(stack, appContainerName, ecsConfig)
            this.targetService = this.appLbFargateService.service
        } else {
            this.targetService = ecsConfig.targetService!
        }

        const runMigLog = new LogGroup(stack, appConfig.owner.buildId + '_runMigLog', {
            removalPolicy: appConfig.ephemeral ? RemovalPolicy.DESTROY : RemovalPolicy.SNAPSHOT
        });
        const runMigProj = new PipelineProject(stack, appConfig.owner.buildId + 'run_mig_img_def', {
            buildSpec: BuildSpec.fromObject({
                version: '0.2',
                phases: {
                    "install": {
                        "runtime-versions": {"nodejs": 16},
                        "commands": [
                            "npm install -g typescript",
                            "npm install -g ts-node",
                            "npm install aws-sdk",
                            "npm install -g cross-env",
                            `echo '${runMigTsk}' | base64 -d > index.js`
                        ]
                    },
                    build: {
                        commands: [
                            `ls -ltarh $CODEBUILD_SRC_DIR/`,
                            `ecrImgDetail=$CODEBUILD_SRC_DIR/imageDetail.json migTaskDefArn=${ecsConfig.migTaskDef.taskDefinitionArn} output=${taskdefImgFileName} ts-node index.js`,
                            // `cat ${taskdefImgFileName}`
                        ]
                    }
                },
            }),
            environment: {
                buildImage: LinuxBuildImage.AMAZON_LINUX_2_4,
                computeType: ComputeType.MEDIUM
            },
            environmentVariables: {
                ecrUri: {type: BuildEnvironmentVariableType.PLAINTEXT, value: ecsConfig.appImgRepo.repositoryUri},
                taskDefArn: {
                    type: BuildEnvironmentVariableType.PLAINTEXT,
                    value: ecsConfig.migTaskDef.taskDefinitionArn
                },
                migTaskSubnets: {
                    type: BuildEnvironmentVariableType.PLAINTEXT,
                    value: migTaskSubnets.map(s => s.subnetId).join(',')
                },
                migTaskSGs: {
                    type: BuildEnvironmentVariableType.PLAINTEXT,
                    value: ecsConfig.migTaskSGs.map(sg => sg.securityGroupId).join(',')
                },
                migTaskCluster: {
                    type: BuildEnvironmentVariableType.PLAINTEXT,
                    value: this.targetService.cluster.clusterArn
                },
                migTaskTimeOutMin: {
                    type: BuildEnvironmentVariableType.PLAINTEXT,
                    value: 5
                }
            },
            logging: {
                cloudWatch: {logGroup: buildDefLog}
            },
        });
        Grant.addToPrincipal({
            grantee: runMigProj,
            actions: ['ecs:DescribeTaskDefinition', 'ecs:DescribeTasks'],
            resourceArns: ['*'],
            scope: stack
        });
        Grant.addToPrincipal({
            grantee: runMigProj,
            actions: ['iam:PassRole'],
            resourceArns: [ecsConfig.migTaskDef.taskRole!.roleArn, ecsConfig.migTaskDef.executionRole!.roleArn],
            scope: stack
        });
        Grant.addToPrincipal({
            grantee: runMigProj,
            actions: ['ecs:RunTask'],
            resourceArns: [ecsConfig.migTaskDef.taskDefinitionArn],
            scope: stack
        });
        Grant.addToPrincipal({
            grantee: runMigProj,
            actions: ['logs:CreateLogStream', 'logs:PutLogEvents'],
            resourceArns: [runMigLog.logGroupArn, runMigLog.logGroupArn + '*'],
            scope: stack
        });
        Grant.addToPrincipal({
            grantee: buildImgDefProj,
            actions: ['logs:CreateLogStream', 'logs:PutLogEvents'],
            resourceArns: [buildDefLog.logGroupArn, buildDefLog.logGroupArn + '*'],
            scope: stack
        });


        const ecrSrcRole = new Role(stack, 'ecrSrcRole', {assumedBy: new AccountPrincipal(stack.account)});
        if (ecsConfig.appImgRepo instanceof ecr.Repository) {
            ecsConfig.appImgRepo.grantPull(ecrSrcRole)
        } else {
            ecrSrcRole.addToPolicy(new PolicyStatement({
                actions: [
                    "ecr:BatchGetImage",
                    "ecr:GetDownloadUrlForLayer",
                    "ecr:GetRepositoryPolicy",
                    "ecr:DescribeImages",
                ],
                resources: [ecsConfig.appImgRepo.repositoryArn]
            }))
        }
        if (ecsConfig.migImgRepo instanceof ecr.Repository) {
            ecsConfig.migImgRepo.grantPull(ecrSrcRole)
        }
        else{
            ecrSrcRole.addToPolicy(new PolicyStatement({
                actions: [
                    "ecr:BatchGetImage",
                    "ecr:GetDownloadUrlForLayer",
                    "ecr:GetRepositoryPolicy",
                    "ecr:DescribeImages",
                ],
                resources: [ecsConfig.migImgRepo.repositoryArn]
            }))
        }

        this.ecsDeployAction = new EcsDeployAction({
            actionName: 'deployToECS',
            service: this.targetService,
            input: imgFromEcrDef,
        });
        this.deployImgPipeline = new pipeline.Pipeline(stack, appConfig.owner.buildId + '_deploy_img_pipeline', {
            pipelineName: appConfig.owner.buildId + `${OndemandContracts.REV_REF_value.substring(2)}_deploy_img`,
            stages: [
                {
                    stageName: 'Source',
                    actions: [
                        new EcrSourceAction({
                            actionName: 'app_Img_Source',
                            repository: ecsConfig.appImgRepo,
                            imageTag: `${ecsConfig.appImgTag}`,
                            role: ecrSrcRole,
                            output: appImgFromECR
                        }),
                        new EcrSourceAction({
                            actionName: 'mig_Img_Source',
                            repository: ecsConfig.migImgRepo,
                            imageTag: `${ecsConfig.migImgTag}`,
                            role: ecrSrcRole,
                            output: migImgFromECR
                        })
                    ],
                },
                {
                    stageName: 'Build',
                    actions: [
                        new CodeBuildAction({
                            actionName: 'build_app_img_def',
                            input: appImgFromECR,
                            project: buildImgDefProj,
                            outputs: [imgFromEcrDef]
                        }),
                        new CodeBuildAction({
                            actionName: 'run_lastest_mig_task',
                            input: migImgFromECR,
                            project: runMigProj,
                            // outputs: [imgFromEcrDef]
                        }),
                    ],
                },
                {
                    stageName: 'Deploy',
                    actions: [
                        this.ecsDeployAction,
                    ],
                }
            ],
            artifactBucket: new Bucket(stack, appConfig.owner.buildId + '_deploy_img_pipeline_artifact', {
                removalPolicy: appConfig.ephemeral ? RemovalPolicy.DESTROY : RemovalPolicy.RETAIN,
                autoDeleteObjects: appConfig.ephemeral
            })
        });

        this.deployImgPipeline.artifactBucket.applyRemovalPolicy(appConfig.ephemeral ? RemovalPolicy.DESTROY : RemovalPolicy.RETAIN)

        // this.targetService.node.addDependency( this.deployImgPipeline )

        ecsConfig.appImgRepo.grantPullPush(buildImgDefProj)
        ecsConfig.appImgRepo.grantPull(this.targetService.taskDefinition.obtainExecutionRole())
    }

    readonly appLbFargateService: ApplicationLoadBalancedFargateService

    private createEcsService(stack: Stack, ecsContainerName: string | undefined, ecsConfig: EcrECSConfig) {
        const taskDefinition = new ecs.FargateTaskDefinition(stack, 'fargateTaskDef');
        taskDefinition.addContainer('container', {
            containerName: ecsContainerName,
            image: ContainerImage.fromRegistry(EcrECS.PLACE_HOLDER_IMG),
            logging: ecs.LogDriver.awsLogs({streamPrefix: ecsConfig.appImgRepo.repositoryName})
        }).addPortMappings({hostPort: 8080, containerPort: 8080})

        return new ApplicationLoadBalancedFargateService(stack, 'albfs', {
            vpc: ecsConfig.vpc,
            assignPublicIp: true,
            taskDefinition,
            listenerPort: 8080,
            platformVersion: FargatePlatformVersion.VERSION1_4,
            desiredCount: 1
        })
    }
}
