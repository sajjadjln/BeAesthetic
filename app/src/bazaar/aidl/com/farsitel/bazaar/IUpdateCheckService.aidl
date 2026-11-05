// Cafe Bazaar's update check service, as published in github.com/cafebazaar/BazaarUpdater
// (Apache 2.0). Only the first two methods; their order fixes the binder codes, so keep it.
package com.farsitel.bazaar;

interface IUpdateCheckService {
    long getVersionCode(String packageName);
    long getRemoteVersionCode(String packageName);
}
