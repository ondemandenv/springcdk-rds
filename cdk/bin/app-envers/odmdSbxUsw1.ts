import {AppEnverConfig} from "../../lib/app-enver-config";
import {CurrentEnver} from "../app";
import {OndemandContracts, PgUsr} from "@ondemandenv/odmd-contracts";

export class EnverConfigImpl implements AppEnverConfig {
    appUsr: PgUsr = {
        roleType: "app",
        userName: ('appUsr' + OndemandContracts.REV_REF_value.substring(2) + '_19').toLowerCase()
    };
    migUsr: PgUsr = {
        roleType: "migrate",
        userName: ('migUsr' + OndemandContracts.REV_REF_value.substring(2) + '_19').toLowerCase()
    };
    // readUsr: PgUsr = {role: "readonly", userName: 'readonly_' + CurrentEnver.srcBranch};
}
