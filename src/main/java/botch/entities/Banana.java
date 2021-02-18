package botch.entities;

import com.google.gson.annotations.SerializedName;

public class Banana {
    @SerializedName("damage")
    public int damage;

    @SerializedName("range")
    public int range;

    @SerializedName("count")
    public int count;

    @SerializedName("damageRadius")
    public int damageRadius;
}
