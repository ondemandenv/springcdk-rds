function fn() {
    // Auth token set in karate-config.js
    let token = karate.get('token');
    if (token) {
        return {
            Authorization: 'Bearer ' + token
        };
    } else {
        return {};
    }
}