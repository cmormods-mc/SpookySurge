package com.bossbarskins;

/**
 * A reskin a mod declares in its {@code bossbarskins.json}: which bar the mod's code asks for ({@code requested}) and the sprites to show.
 * A transparent skin has no sprites: it asks for an invisible bar (used as a spacer to push the next bar down), and every mod's
 * transparent request shares one slot.
 */
public record Skin(String owner, String id, Slot requested, boolean transparent, boolean opaque, String background, String progress, String meta) {
    public String key() {
        return owner + ":" + id;
    }
}
