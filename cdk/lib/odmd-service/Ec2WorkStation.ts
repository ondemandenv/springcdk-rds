import {Construct} from "constructs";
import {CfnOutput, Stack} from "aws-cdk-lib";
import {
    BastionHostLinux,
    BlockDeviceVolume, CfnKeyPair,
    InstanceClass,
    InstanceSize,
    InstanceType,
    IVpc, MachineImage, Port, SecurityGroup, SelectedSubnets,
    SubnetType, Vpc
} from "aws-cdk-lib/aws-ec2";
import {ManagedPolicy, Role} from "aws-cdk-lib/aws-iam";
import * as codecommit from "aws-cdk-lib/aws-codecommit";
import {Bucket} from "aws-cdk-lib/aws-s3";
import {BucketDeployment, Source} from "aws-cdk-lib/aws-s3-deployment";
import * as fs from "fs";
import {ContractsEnverCdk, OdmdNames, OndemandContracts, WithRds} from "@ondemandenv/odmd-contracts";
import {ContractsCrossRefConsumer} from "@ondemandenv/odmd-contracts/lib/odmd-model/contracts-cross-refs";

export class Ec2WorkStation<T extends ContractsEnverCdk> extends Construct {

    readonly bastion: BastionHostLinux

    constructor(scope: Stack, id: string, rdsPortConsumer: ContractsCrossRefConsumer<T, any>, rdsHostConsumer: ContractsCrossRefConsumer<T, any>) {
        super(scope, id);

        const odmdEnverVpc = rdsPortConsumer.owner as any as WithRds;
        if (!odmdEnverVpc.vpcConfig) {
            throw new Error('Need vpc')
        }
        const vpc: IVpc = Vpc.fromLookup(this, 'vpc', {vpcName: odmdEnverVpc.vpcConfig!.vpcName!})
        let ec2Subnets: SelectedSubnets;
        try {
            ec2Subnets = vpc.selectSubnets({subnetType: SubnetType.PRIVATE_WITH_EGRESS});
        } catch (e) {
            console.warn(`try PRIVATE_ISOLATED when can't find PRIVATE_WITH_EGRESS: ${(e as any).message}`)
            ec2Subnets = vpc.selectSubnets({subnetType: SubnetType.PRIVATE_ISOLATED})
        }
        this.bastion = new BastionHostLinux(this, 'payments-bastion', {
            vpc,
            machineImage: MachineImage.fromSsmParameter('/aws/service/canonical/ubuntu/eks-pro/22.04/stable/20240410/amd64/hvm/ebs-gp2/ami-id'),
            instanceName: OdmdNames.create(this, 'ec2-inst'),
            subnetSelection: ec2Subnets,
            instanceType: InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.LARGE),
            blockDevices: [{deviceName: '/dev/xvda', volume: BlockDeviceVolume.ebs(64)}]
        });
        const bastion = this.bastion
        bastion.instance.role.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName('AmazonSSMManagedInstanceCore'));

        const keyName = OdmdNames.create(this, 'ec2KeyPair');
        new CfnKeyPair(this, 'bastionKey', {keyName, tags: [{key: 'keyName', value: keyName}]})

        bastion.instance.instance.addPropertyOverride('KeyName', keyName);

        const ccRepoName = rdsPortConsumer.owner.owner.buildId
        const appInfraRepoArn = `arn:aws:codecommit:${scope.region}:${OndemandContracts.inst.accounts.central}:${ccRepoName}`;
        const infraRepo = codecommit.Repository.fromRepositoryArn(this, 'appInfraRepo', appInfraRepoArn) as codecommit.Repository;


        infraRepo.grantPull(bastion)
        infraRepo.grantRead(bastion)

        const sreAppRepOwner = Role.fromRoleArn(this, 'infra-repo-owner', rdsPortConsumer.owner.centralRepoSrcRoleArn) as Role

        sreAppRepOwner.grantAssumeRole(bastion.instance.role)

        const bucketKey = `ec2-setup/ts-node-aws-creds.js`;
        const bucket = new Bucket(this, 'ec2-setup-bucket');
        bucket.grantRead(bastion)
        const bd = new BucketDeployment(this, 'ec2-setup-bucketdeployment', {
            sources: [Source.data(bucketKey, fs.readFileSync(`${__dirname}/ec2-utils/assumerole-awsprofile.js`, {encoding: 'utf-8'}))],
            destinationBucket: bucket
        })

        bastion.node.addDependency(bd)

        const ec2ideEntry = '/home/ubuntu/genAwsProfile.js';
        bastion.instance.userData.addS3DownloadCommand({bucketKey, bucket, localFile: ec2ideEntry})

        const awsCodeCommitProfile = `aws_codecommit_${ccRepoName}`
        const nodeVer = 'node-v20.12.2-linux-x64'
        bastion.instance.userData.addCommands(
            `sudo -u ubuntu bash -c "mkdir /home/ubuntu/.aws"`,
            `sudo -u ubuntu bash -c "touch /home/ubuntu/.aws/credentials"`,
            `sudo -u ubuntu bash -c "chmod 600 /home/ubuntu/.aws/credentials"`,

            'apt-get update -y',
            'apt-get install git -y',
            `wget -O - https://apt.corretto.aws/corretto.key | sudo gpg --dearmor -o /usr/share/keyrings/corretto-keyring.gpg && \\
echo "deb [signed-by=/usr/share/keyrings/corretto-keyring.gpg] https://apt.corretto.aws stable main" | sudo tee /etc/apt/sources.list.d/corretto.list`,

            `sudo -u ubuntu bash -c "mkdir /home/ubuntu/.nodejs"`,
            `wget https://nodejs.org/dist/v20.12.2/${nodeVer}.tar.xz -P /home/ubuntu`,
            `tar -xvf /home/ubuntu/${nodeVer}.tar.xz -C /home/ubuntu/.nodejs`,
            `chown -R ubuntu:ubuntu /home/ubuntu/.nodejs`,
            `echo "export PATH=/home/ubuntu/.nodejs/${nodeVer}/bin:$PATH" >>  /home/ubuntu/.bashrc`,

            `sudo -u ubuntu bash -c "touch /home/ubuntu/genAwsConfig.sh"`,
            `echo "source /home/ubuntu/.bashrc" >> /home/ubuntu/genAwsConfig.sh`,
            `echo "npm install -g typescript" >> /home/ubuntu/genAwsConfig.sh`,
            `echo "npm install -g ts-node" >> /home/ubuntu/genAwsConfig.sh`,

            `echo "roleArn=${rdsPortConsumer.owner.centralRepoSrcRoleArn} sessionName=tmpssnn profile=${awsCodeCommitProfile} path=/home/ubuntu/.aws/credentials ts-node ${ec2ideEntry}" >> /home/ubuntu/genAwsConfig.sh`,

            `chmod +x /home/ubuntu/genAwsConfig.sh`,
            `sudo -u ubuntu bash -c "/home/ubuntu/genAwsConfig.sh"`,

            `echo "export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto.x86_64" >>  /home/ubuntu/.bashrc`,
            `echo "echo PATH=$PATH:$JAVA_HOME/bin" >>  /home/ubuntu/.bashrc`,

            `sudo -u ubuntu bash -c "git config --global credential.helper '!aws  --profile ${awsCodeCommitProfile} codecommit credential-helper $@'"`,
            `sudo -u ubuntu bash -c "git config --global credential.UseHttpPath true"`,
            `sudo -u ubuntu bash -c "git clone -v https://git-codecommit.${scope.region}.amazonaws.com/v1/repos/${ccRepoName} /home/ubuntu/${ccRepoName}"`,

            `echo "export spring_profiles_active=local" >>  /home/ubuntu/.bashrc`
        )
        if (odmdEnverVpc.rdsConfig) {
            // const rdsPort = odmdEnver.getSharedValue(Stack.of(this), odmdEnverVpc.rdsConfig.clusterPort);
            const rdsPort = rdsPortConsumer.getSharedValue(scope)
            // const rdsHost = odmdEnver.getSharedValue(Stack.of(this), odmdEnverVpc.rdsConfig.clusterHostname) as string;
            const rdsHost = rdsHostConsumer.getSharedValue(scope)
            const rdsSg = SecurityGroup.fromLookupByName(this, 'rds-sg', odmdEnverVpc.rdsConfig.defaultSgName, vpc)

            rdsSg.connections.allowFrom(bastion, Port.tcp(rdsPort as any))

            bastion.instance.userData.addCommands(
                `echo "export RDS_ENDPOINT=${rdsHost + '/' + odmdEnverVpc.rdsConfig.defaultDatabaseName}" >>  /home/ubuntu/.bashrc`,
                `echo "export RDS_PORT=${rdsPort}" >>  /home/ubuntu/.bashrc`
            )

            const ec2Ssmdoc = `--document-name AWS-StartPortForwardingSession`;
            const rdsSsmDoc = `--document-name AWS-StartPortForwardingSessionToRemoteHost`;

            const ec2Params = `--parameters "localPortNumber=????,portNumber=22"`;
            const rdsSsmParams = `--parameters "host"="${rdsHost}","portNumber"=["${rdsPort}"],"localPortNumber"=["????"]`;

            new CfnOutput(this, 'ec2-tunnel', {
                value: `aws ssm start-session --target ${bastion.instanceId} ${ec2Ssmdoc} ${ec2Params}  --profile ?`
            })

            new CfnOutput(this, 'rds-tunnel', {
                value: `aws ssm start-session --target ${bastion.instanceId} ${rdsSsmDoc}  ${rdsSsmParams} --profile ?`
            })

        }
    }
}
