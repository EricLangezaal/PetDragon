package com.ericdebouwer.petdragon;

public enum Message {
	NO_PERMISSION_COMMAND("no-command-permission"),NO_RIDE_PERMISSION ("no-ride-permission"),
	DRAGON_SPAWNED ("dragon-spawned"), DRAGON_REMOVED ("dragon-removed"), DRAGON_NOT_FOUND ("no-dragon-found-to-remove"),
	COMMAND_USAGE("command-usage");
	
	String key;
	Message(String key){
		this.key = key;
	}
	public String getKey(){ return this.key;}
}
