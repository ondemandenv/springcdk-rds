#todo: user input 1)mandatory: target rev; 2)optional: one of this file to override the default( defined in contracts ), to deploy an env

env
ls -ltarh
pwd
npm install
npm run build
cross-env CDK_DEBUG=true cdk  deploy  $SYNTH_TARGET   --require-approval never --no-rollback -vvv
