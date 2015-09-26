package com.named_data.ndnhome;

public class AccessTokenInfo {
	private String command;
	private int commandTokenSequence;
	private int seedSequence;
	private byte[] accessToken;
	private String accessTokenName;
	
	public AccessTokenInfo(String command, int commandTokenSequence, int seedSequence, byte[] accessToken, String accessTokenName) {
		this.command = command;
		this.commandTokenSequence = commandTokenSequence;
		this.seedSequence = seedSequence;
		this.accessToken = accessToken;
		this.accessTokenName = accessTokenName;
	}
	
	public String getCommandName() {
		return command;
	}
	
	public int getCommandTokenSequence(){
        return commandTokenSequence;		
	}
	
	public int getSeedSequence(){
		return seedSequence;
	}
	
	public byte[] getAccessToken(){
		return accessToken;
	}
	
	public String getAccessTokenName(){
		return accessTokenName;
	}
	
}
