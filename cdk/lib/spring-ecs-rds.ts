import {Duration, RemovalPolicy, Stack, StackProps} from "aws-cdk-lib";
import {Construct} from "constructs";
import {Port, SecurityGroup, Vpc,} from "aws-cdk-lib/aws-ec2";
import {Bucket} from "aws-cdk-lib/aws-s3";
import {CurrentEnver} from "../bin/app";
import {ContainerImage, FargatePlatformVersion, FargateTaskDefinition, LogDriver} from "aws-cdk-lib/aws-ecs";
import {ApplicationLoadBalancer} from "aws-cdk-lib/aws-elasticloadbalancingv2";
import {ApplicationLoadBalancedFargateService} from "aws-cdk-lib/aws-ecs-patterns";
import {IServerlessCluster, ServerlessCluster} from "aws-cdk-lib/aws-rds";
import {PgSchemaUsers, PgSchemaUsersProps} from "@ondemandenv/odmd-contracts";
import {Secret} from "aws-cdk-lib/aws-secretsmanager";
import {DockerECR} from "./odmd-service/DockerECR";
import {EcrECS} from "./odmd-service/EcrECS";
import {Ec2WorkStation} from "./odmd-service/Ec2WorkStation";
import {Repository} from "aws-cdk-lib/aws-ecr";

export class SpringEcsRds extends Stack {

    public readonly rdsCluster: IServerlessCluster;

    constructor(scope: Construct, id: string, props?: StackProps) {
        super(scope, id, props);


        const enver = CurrentEnver.odmdEnVerConfig;
        const localConfig = CurrentEnver.appEnverConfig;
        const vpc = Vpc.fromLookup(this, 'vpc', {
            vpcName: enver.vpcConfig.vpcName
        })

        this.rdsCluster = ServerlessCluster.fromServerlessClusterAttributes(this, 'clstr', {clusterIdentifier: enver.rdsConfig!.clusterIdentifier})

        const imgSrcEnver = enver.imgSrcEnver;

        const migTaskDef = new FargateTaskDefinition(this, 'migTaskDef');

        const rdsConfig = enver.rdsConfig!;

        const pgUsers = new PgSchemaUsers(this, new PgSchemaUsersProps(enver,
            enver.pgSchemaUsersProps.schema, [
                {
                    userName: localConfig.appUsr.userName,
                    roleType: 'app'
                }, {
                    userName: localConfig.migUsr.userName,
                    roleType: 'migrate'
                }
            ]
        ))

        const migRepoName = enver.migImgName.getSharedValue(this)
        const migImgRepo = Repository.fromRepositoryAttributes(this, 'mig-repo', {
            repositoryArn: `arn:aws:ecr:${this.region}:${imgSrcEnver.targetAWSAccountID}:repository/${migRepoName}`,
            repositoryName: migRepoName
        }) as Repository

        const appRepoName = enver.appImgName.getSharedValue(this)
        const appImgRepo = Repository.fromRepositoryAttributes(this, 'app-repo', {
            repositoryArn: `arn:aws:ecr:${this.region}:${imgSrcEnver.targetAWSAccountID}:repository/${appRepoName}`,
            repositoryName: appRepoName
        }) as Repository

        const rdsSocketAddress = enver.rdsSocketAddress.getSharedValue(this)
        migTaskDef.addContainer('mig-container', {
            containerName: 'mig',
            image: ContainerImage.fromEcrRepository(migImgRepo),
            logging: LogDriver.awsLogs({
                streamPrefix: migImgRepo.repositoryName
            }),
            environment: {
                RDS_ENDPOINT: rdsSocketAddress + '/' + rdsConfig.defaultDatabaseName,
                RDS_SECRET: pgUsers.usernameToSecretId.get(localConfig.migUsr.userName)!,
                APP_SCHEMA: enver.pgSchemaUsersProps.schema,
                read_only: enver.rdsUsrReadOnly.getSharedValue(this)
            }
        });

        Secret.fromSecretNameV2(this, 'mig-seret', pgUsers.usernameToSecretId.get(localConfig.migUsr.userName)!).grantRead(migTaskDef.taskRole)

        const rdsSg = SecurityGroup.fromLookupByName(this, 'rds-sg', enver.rdsConfig.defaultSgName, vpc)

        const migSg = new SecurityGroup(this, 'mig-sg', {vpc})
        const rdsPort = enver.rdsPort.getSharedValue(this);
        rdsSg.connections.allowFrom(migSg, Port.tcp(rdsPort as any))

        const appTaskDef = new FargateTaskDefinition(this, 'appTaskDef', {
            cpu: 1024, memoryLimitMiB: 2048
        });
        const springListenerPort = 8080;
        appTaskDef.addContainer('app-container', {
            containerName: 'app',
            image: ContainerImage.fromEcrRepository(appImgRepo),
            logging: LogDriver.awsLogs({
                streamPrefix: appImgRepo.repositoryName
            }),
            cpu: 1024, memoryLimitMiB: 2048,
            environment: {
                RDS_ENDPOINT: rdsSocketAddress + '/' + rdsConfig.defaultDatabaseName,
                RDS_SECRET: pgUsers.usernameToSecretId.get(localConfig.appUsr.userName)!,
                APP_SCHEMA: enver.pgSchemaUsersProps.schema,
                OTEL_TRACE_AGENT_URL: 'http://localhost:9411/v1/trace',
                OTEL_SERVICE_NAME: 'rds_spring_ecs',
                SPRING_CLOUD_VAULT_ENABLED: 'false',
                SPRING_CLOUD_KUBERNETES_ENABLED: 'false'
            }
        }).addPortMappings({hostPort: springListenerPort, containerPort: springListenerPort});

        Secret.fromSecretNameV2(this, 'app-seret', pgUsers.usernameToSecretId.get(localConfig.appUsr.userName)!).grantRead(appTaskDef.taskRole)

        const applicationLoadBalancer = new ApplicationLoadBalancer(this, 'ecs-fargate-service-alb', {vpc});
        applicationLoadBalancer.logAccessLogs(new Bucket(this, 'ecs-fargate-service-alb-log', {
            removalPolicy: RemovalPolicy.DESTROY,
            autoDeleteObjects: true
        }))

        const albFargateSg = new SecurityGroup(this, 'ecs-fargate-service-sg', {vpc})
        const albFargate = new ApplicationLoadBalancedFargateService(this, 'ecs-fargate-service', {
            vpc: vpc,
            assignPublicIp: false,
            taskDefinition: appTaskDef,
            listenerPort: springListenerPort,
            platformVersion: FargatePlatformVersion.VERSION1_4,
            healthCheckGracePeriod: Duration.minutes(1),
            loadBalancer: applicationLoadBalancer,
            securityGroups: [albFargateSg]
        });
        rdsSg.connections.allowFrom(albFargateSg, Port.tcp(rdsPort as any))

        albFargate.targetGroup.configureHealthCheck({
            path: '/actuator/health',
            timeout: Duration.seconds(15),
            healthyThresholdCount: 3
        })

        const ee = new EcrECS(this, {
            vpc,
            appContainerName: 'app',
            appImgRepo,
            appImgTag: DockerECR.LATEST_IMG_TAG,

            targetService: albFargate.service,

            migImgRepo,
            migImgTag: DockerECR.LATEST_IMG_TAG,
            migTaskDef,
            migTaskSGs: [migSg]
        }, enver);


        // new OdmdCentralConfigNetworking().buildEnverConfig.find( c=>c.baseBranch=='weqr')?.ipamPoolShareName

        // this.rdsCluster.dbClusterSG.addIngressRule(Peer.ipv4('10.160.0.112/28'), Port.tcp(CurrentEnver.appEnverConfig.rdsConfig!.databasePort))//todo: abstract this

        const ws = new Ec2WorkStation(this, 'ec2-ws', enver.rdsPort, enver.rdsHost)


        // let iCluster = Cluster.fromClusterAttributes(this, 'cluster', {
        //     clusterName: OdmdBuildConfig.inst.eksConfig.
        /*CurrentEnver.odmdEnVerConfig.getSharedValue(
            this, OdmdBuildConfig.inst.networkingConfig.ipam_west1_le.ipamPoolName),*/
        // });
        // iCluster.addCdk8sChart( 'cdk8s', new cdk8s.Chart( new cdk8s.App(), 'tmp') )
    }
}
