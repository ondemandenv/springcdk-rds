#!/usr/bin/env ts-node

import * as fs from "fs";

async function main() {
    console.log( process.env )

    const ecrImgDetail = process.env.ecrImgDetail!
    const img = JSON.parse(fs.readFileSync(ecrImgDetail, {encoding: 'utf-8'})) as {
        RepositoryName:string,
        ImageTags:string[]
    }

    const ecsContainerName = process.env.ecsContainerName!
    const output = process.env.output!

    const ecrUri = process.env.ecrUri!
    fs.writeFileSync(output,
        `[{"name":"${ecsContainerName}","imageUri": "${ecrUri}:${img.ImageTags.find( t=>t!='latest')}"}]`
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