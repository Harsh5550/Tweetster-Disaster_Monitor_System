package com.harsh.tweetster.v1.models;

import java.io.Serializable;

public class Cluster implements Serializable {
    public String id, latitude, longitude, timeStamp, userId;
    public long count;
}
