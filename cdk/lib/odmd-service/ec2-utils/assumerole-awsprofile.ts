#!/usr/bin/env ts-node


import * as fs from "fs";
import {exec} from "child_process";

//ts-node not worth it with types ... https://github.com/TypeStrong/ts-node#missing-types
//import {AssumeRoleCommand, GetCallerIdentityCommand, STSClient} from "@aws-sdk/client-sts";

function execShellCommand(cmd: string): Promise<string> {
    return new Promise((resolve, reject) => {
        exec(cmd, (error: any, stdout: string, stderr: string) => {
            if (error) {
                reject(error)
            }
            resolve(stdout ? stdout : stderr);
        });
    });
}


async function main() {
    const assumeRoleArn = process.env.roleArn!
    const roleSessionName = process.env.sessionName!
    const awsProfile = process.env.profile!
    const awsCredentialPath = process.env.path!

    const cmd = `aws sts assume-role --role-arn=${assumeRoleArn} --role-session-name="${roleSessionName}"`;
    const asmRslt: any = JSON.parse(await execShellCommand(cmd))

    fs.writeFileSync(awsCredentialPath,
        `
[${awsProfile}]
aws_access_key_id=${asmRslt.Credentials!.AccessKeyId}
aws_secret_access_key=${asmRslt.Credentials!.SecretAccessKey}
aws_session_token=${asmRslt.Credentials!.SessionToken}
`
    )
}

console.log("main begin.")
main().catch(e => {
    console.error("main e>>>")
    console.error(e)
    console.error("main e<<<")
    throw e
}).finally(() => {
    console.log("main end.")
})