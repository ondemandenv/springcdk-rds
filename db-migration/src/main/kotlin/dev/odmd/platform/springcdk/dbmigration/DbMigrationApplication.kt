package dev.odmd.platform.springcdk.dbmigration

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DbMigrationApplication

fun main(args: Array<String>) {
    runApplication<DbMigrationApplication>(*args)
    System.exit(0)
}
