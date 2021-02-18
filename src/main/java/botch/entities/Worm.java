package botch.entities;

import com.google.gson.annotations.SerializedName;

public class Worm {
    @SerializedName("id")
    public int id;

    @SerializedName("health")
    public int health;

    @SerializedName("position")
    public Position position;

    @SerializedName("diggingRange")
    public int diggingRange;

    @SerializedName("movementRange")
    public int movementRange;

    @SerializedName("bananaBombs")
    public Banana bananas;

    @SerializedName("snowballs")
    public Snowball snowball;

    @SerializedName("roundUntilUnfrozen")
    public int roundUntilUnfrozen;

    @SerializedName("profession")
    public String profession;

}
