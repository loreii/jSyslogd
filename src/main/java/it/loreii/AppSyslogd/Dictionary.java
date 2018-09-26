package it.loreii.AppSyslogd;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.annotation.Id;

public class Dictionary {
	@Id
	private String id;
	private String type;
	private String value;
	
	public Dictionary() {
		//JPA
	}
	
	public Dictionary(String type, String value) {
		super();
		this.type = type;
		this.value = value;
		id=DigestUtils.sha1Hex(type+value);
	}

	public String getType() {
		return type;
	}

	public String getValue() {
		return value;
	}
	
}
