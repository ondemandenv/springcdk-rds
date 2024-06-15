rootProject.name = "odmd-springcdk-example"

include( "cdk", "api", "app", "db-migration", "domain", "gateways", "common", "karate", "webhooks", "services", "events-publisher", "ui")

project(":api").name = "cdk-spring-rds-api"
project(":app").name = "cdk-spring-rds-app"
project(":db-migration").name = "cdk-spring-rds-db-migration"
project(":domain").name = "cdk-spring-rds-domain"
project(":gateways").name = "cdk-spring-rds-gateways"
project(":common").name = "cdk-spring-rds-common"
project(":karate").name = "cdk-spring-rds-karate"
project(":webhooks").name = "cdk-spring-rds-webhooks"
project(":services").name = "cdk-spring-rds-services"
project(":events-publisher").name = "cdk-spring-rds-events-publisher"
project(":ui").name = "cdk-spring-rds-ui"
