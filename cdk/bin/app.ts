import * as cdk from 'aws-cdk-lib';
import {StackProps} from 'aws-cdk-lib';
import {AppEnverConfig} from "../lib/app-enver-config";
import {SpringEcsRds} from "../lib/spring-ecs-rds";
import {OdmdEnverSampleSpringCdkEcs, OndemandContracts} from "@ondemandenv/odmd-contracts";

if (!OndemandContracts.REV_REF_value) {
    throw new Error(`this build needs environment variable ${OndemandContracts.REV_REF_name} to dynamically load configs, please set it correctly`)
}

export class CurrentEnver {

    static async init() {
        if (CurrentEnver._odmdEnVerConfig) {
            throw new Error("can't init twice")
        }

        CurrentEnver._odmdEnVerConfig = OndemandContracts.inst.springRdsCdk.envers.find(e => {
            return OndemandContracts.REV_REF_value == e.targetRevision.toPathPartStr()
        })! as OdmdEnverSampleSpringCdkEcs

        if (!CurrentEnver._odmdEnVerConfig) {
            throw new Error(`Can't find enver with target_rev_ref: ${OndemandContracts.REV_REF_value}`)
        }

        if (CurrentEnver._appEnverConfig) {
            throw new Error("can't init twice")
        }
        const {EnverConfigImpl} = await import( (`./app-envers/${OndemandContracts.REV_REF_value.substring(3)}`) )
        CurrentEnver._appEnverConfig = new EnverConfigImpl() as AppEnverConfig
    }

    private static _odmdEnVerConfig: OdmdEnverSampleSpringCdkEcs;

    public static get odmdEnVerConfig(): OdmdEnverSampleSpringCdkEcs {
        return this._odmdEnVerConfig;
    }

    private static _appEnverConfig: AppEnverConfig;
    public static get appEnverConfig(): AppEnverConfig {
        return CurrentEnver._appEnverConfig
    }

}


async function main() {

    const app = new cdk.App();

    new OndemandContracts(app)

    const buildRegion = process.env.CDK_DEFAULT_REGION;
    const buildAccount = process.env.CDK_DEFAULT_ACCOUNT
        ? process.env.CDK_DEFAULT_ACCOUNT
        : process.env.CODEBUILD_BUILD_ARN!.split(":")[4];
    if (!buildRegion || !buildAccount) {
        throw new Error("buildRegion>" + buildRegion + "; buildAccount>" + buildAccount)
    }

    const props = {
        env: {
            account: buildAccount,
            region: buildRegion
        }
    } as StackProps;
    await CurrentEnver.init()


    /*
    BuildConfig.getAppConfig()
    BuildConfig.getOdmdEnVerConfig()
    BuildConfig.getSrcBranch()
     */

    const stackNamesPerEnv = CurrentEnver.odmdEnVerConfig.getRevStackNames()

    new SpringEcsRds(app, stackNamesPerEnv[0], props)

    // new App_nameStack(app, stackNamesPerEnv[0], props)
    // new App_nameStack1(app, stackNamesPerEnv[1], props)
    // new App_nameStack2(app, stackNamesPerEnv[2], props)

}


console.log("main begin.")
main().catch(e => {
    console.error(e)
    throw e
}).finally(() => {
    console.log("main end.")
})

