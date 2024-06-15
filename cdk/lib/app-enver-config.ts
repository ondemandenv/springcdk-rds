import {PgUsr} from "@ondemandenv/odmd-contracts";


export interface AppEnverConfig {

    // readonly schema: string
    readonly appUsr: PgUsr
    readonly migUsr: PgUsr
    // readonly readUsr: PgUsr

}
