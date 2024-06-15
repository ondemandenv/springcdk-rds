import {AppEnverConfig} from "../../lib/app-enver-config";
import {CurrentEnver} from "../app";
import {OndemandContracts, PgUsr} from "@ondemandenv/odmd-contracts";

export class EnverConfigImpl implements AppEnverConfig {
    appUsr: PgUsr = {roleType: "app", userName: 'app_' + OndemandContracts.REV_REF_value.substring(2)};
    migUsr: PgUsr = {roleType: "migrate", userName: 'migrate_' + OndemandContracts.REV_REF_value.substring(2)};
    // readUsr: PgUsr = {role: "readonly", userName: 'readonly_' + CurrentEnver.srcBranch};
}
