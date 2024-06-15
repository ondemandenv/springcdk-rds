#!/usr/bin/env ts-node

import * as fs from "fs";

//@ts-ignore
import * as aws from "aws-sdk"

async function main() {
    const ecrImgDetail = process.env.ecrImgDetail!
    const img = JSON.parse(fs.readFileSync(ecrImgDetail, {encoding: 'utf-8'})) as {
        RepositoryName: string,
        ImageTags: string[]
    }

    const taskDefArn = process.env.migTaskDefArn!
    const migTaskSubnets = process.env.migTaskSubnets!
    const migTaskSGs = process.env.migTaskSGs!
    const cluster = process.env.migTaskCluster!
    const taskTimeOutMin = process.env.migTaskTimeOutMin!

    const ecs = new aws.ECS()
    const def = await ecs.describeTaskDefinition({taskDefinition: taskDefArn}).promise()

    console.log(`>>${def}>>`)
    console.log(def)
    console.log(`<<${def}<<`)

    const rslt = await ecs.runTask({
        cluster,
        taskDefinition: def.taskDefinition!.taskDefinitionArn!,
        networkConfiguration: {
            awsvpcConfiguration: {
                subnets: migTaskSubnets.split(','),
                securityGroups: migTaskSGs.split(','),
            },
        },
        launchType: 'FARGATE',
        count: 1,
        // overrides:{containerOverrides:[{name:'', }]}
    }).promise()

    const taskBeginTime = new Date().getTime()
    while (true) {
        //@ts-ignore
        const desc = await ecs.describeTasks({cluster, tasks: rslt.tasks!.map(t => t.taskArn!)}).promise()
        console.log(JSON.stringify(desc))
        if (desc.failures && desc.failures.length > 0) {
            throw new Error('failure detected')
        }

        //@ts-ignore
        const ss = desc.tasks!.reduce((p, v) => {
            p.add(v.lastStatus ?? 'undefined')
            return p
        }, new Set<string>())

        if (ss.size == 1 && ss.has('STOPPED')) {
            break
        }
        if (new Date().getTime() - taskBeginTime > +taskTimeOutMin * 60000) {
            throw new Error("timeout")
        }

        await new Promise(f => setTimeout(f, 2222))
    }

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