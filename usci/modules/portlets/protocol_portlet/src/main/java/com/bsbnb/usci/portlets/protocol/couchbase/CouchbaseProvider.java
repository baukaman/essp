package com.bsbnb.usci.portlets.protocol.couchbase;

import com.couchbase.client.CouchbaseClient;
import com.google.gson.Gson;
import kz.bsbnb.usci.eav.model.json.BatchFullJModel;

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;

public class CouchbaseProvider {
    private static CouchbaseClient couchbaseClient;

    private static Gson gson = new Gson();

    private static CouchbaseProvider provider = null;

    public CouchbaseProvider() {

    }

    public static CouchbaseProvider getInstance() {
        if(provider == null) {
            System.setProperty("viewmode", "production");

            ArrayList<URI> nodes = new ArrayList<URI>();
            nodes.add(URI.create("http://127.0.0.1:8091/pools"));

            try {
                couchbaseClient = new CouchbaseClient(nodes, "test", "");
            } catch (Exception e) {
                e.printStackTrace();
            }

            provider = new CouchbaseProvider();
        }

        return provider;
    }

    public BatchFullJModel getBatchFullModel(BigInteger batchId) {
        Object obj = couchbaseClient.get("batch:" + batchId);

        return gson.fromJson(obj.toString(), BatchFullJModel.class);
    }

}