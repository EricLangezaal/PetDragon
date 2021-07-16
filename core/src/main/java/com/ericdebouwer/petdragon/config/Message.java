package com.ericdebouwer.petdragon.config;

public enum Message {
	NO_PERMISSION_COMMAND("no-command-permission"),
	NO_RIDE_PERMISSION ("no-ride-permission"),
	NO_JOYRIDE ("no-joyride-permission"),
	NO_EGG("no-egg-permission"),
	EGG_HATCHED("egg-hatched"),
	EGG_RECEIVED("egg-received"),
	DRAGON_SPAWNED ("dragon-spawned"),
	DRAGON_REMOVED ("dragon-removed"),
	DRAGON_NOT_FOUND ("no-dragon-found-to-remove"),
	RANGE_INVALID("remove-range-invalid"),
	NOT_YOURS_TO_REMOVE ("not-yours-to-remove"),
	RELOAD_SUCCESS ("reload-success"),
	RELOAD_FAIL("reload-fail"),
	DRAGON_LIMIT ("too-many-dragons"),
	NO_LOCATE("locate-no-dragons"),
	LOCATED_DRAGONS("located-dragons-header"),
	LOCATE_ONE("located-a-dragon"),
	LOCATED_HOVER ("located-hover-text"),
	COMMAND_USAGE("command-usage");
	
	String key;
	Message(String key){
		this.key = key;
	}
	public String getKey(){ return this.key;}
}
