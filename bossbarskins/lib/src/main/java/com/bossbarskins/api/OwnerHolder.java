package com.bossbarskins.api;

/** Implemented by every boss event through a mixin: the id of the mod whose code created it. */
public interface OwnerHolder {
    String bossbarskins$getOwner();

    void bossbarskins$setOwner(String modId);
}
