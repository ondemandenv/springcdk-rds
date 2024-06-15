function fn() {
    karate.configure('connectTimeout', 10000);
    karate.configure('readTimeout', 180000);
    karate.configure('ssl', { trustAll: true });

    karate.log('karate.env: ', karate.env);

    let env = karate.env || 'local';

    let config = {
        env: env,
        apiKey: '',
        urlBase: '',
        auth0BaseUrl: '',
        auth0User: 'karate-auth0-user@ondemandenv.dev',
        auth0Password: 'test123',
        auth0ClientId: '',
        auth0ClientSecret: ''
    }
    if (env === 'dev') {
        config.urlBase = 'https://apidev-internal.ondemandenv.dev:8443/payments-platform'
        config.xxxApiKey = 'WexAikQ499HeiSKcqWXjj5M4vdybzNsA'
        config.auth0BaseUrl = 'https://dev-ondemand.auth0.com'
        config.auth0ClientId = 'cI5KTqjWpqUjnnd7eDCIjD75Fp8Znkv8'
        config.auth0ClientSecret = 'B5OOjxRAOTdVWqPNLkfvuPxISz4yTRuJXtwZ-uv-M91T--zxDpeb5lWj1mCLxqNc'
    } else if (env === 'qa') {
        config.urlBase = 'https://apiqa-internal.ondemandenv.dev:8443/payments-platform'
        config.xxxApiKey = 'WexAikQ499HeiSKcqWXjj5M4vdybzNsA'
        config.auth0BaseUrl = 'https://qa-ondemand.auth0.com'
        config.auth0ClientId = 'Pr5KeGQcuB6P0JguFOtpSjfOjjbNaqcG'
        config.auth0ClientSecret = 'IueHZxYYVuASUSC85k1shg3bUhpuJU-DvPUwFI90QtyvVCziv2--AEEz4nYisH8-'
    } else if (env === 'local') {
        config.urlBase = 'http://localhost:8080/payments-platform'
        config.xxxApiKey = 'WexAikQ499HeiSKcqWXjj5M4vdybzNsA'
        config.auth0BaseUrl = 'https://qa-ondemand.auth0.com'
        config.auth0ClientId = 'Pr5KeGQcuB6P0JguFOtpSjfOjjbNaqcG'
        config.auth0ClientSecret = 'IueHZxYYVuASUSC85k1shg3bUhpuJU-DvPUwFI90QtyvVCziv2--AEEz4nYisH8-'
    } else {
        throw "Env '" + env + "' not recognized"
    }

    karate.callSingle('classpath:helpers/pollReadiness.feature', config);

    return config;
}


