package botch.command;

import botch.entities.Position;

public class BananaBombCommand implements Command {
    private Position position;

    public BananaBombCommand(Position position) {this.position = position;}

    @Override
    public  String render() {return String.format("banana %d %d", this.position.x, this.position.y);}
}