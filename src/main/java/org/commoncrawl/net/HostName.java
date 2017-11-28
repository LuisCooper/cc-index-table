package org.commoncrawl.net;

import java.net.URL;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;

import crawlercommons.domains.EffectiveTldFinder;
import crawlercommons.domains.EffectiveTldFinder.EffectiveTLD;

public class HostName {

	public static enum Type {
		hostname,
		IPv4,
		IPv6
	}

	private Type type;
	private String hostName;
	private String[] revHost;
	private String registrySuffix;
	private String domainName;
	private String privateSuffix;
	private String privateDomain;

	private static Pattern SPLIT_HOST_PATTERN = Pattern.compile("\\.");

	/** Pattern to match valid IPv4 addresses */
	public static final Pattern IPV4_ADDRESS_PATTERN = Pattern.compile(
			"(?:(?:25[0-5]|(?:2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(?:25[0-5]|(?:2[0-4]|1{0,1}[0-9]){0,1}[0-9])");

	/** Lazy pattern to catch IPv6 addresses (or what looks similar, does not validate) */
	public static final Pattern IPV6_ADDRESS_PATTERN = Pattern.compile("\\[[0-9a-fA-F:]+\\]");

	public HostName(String hostName) {
		setHostName(hostName);
	}

	public HostName(URL url) {
		String hostName = url.getHost().toLowerCase(Locale.ROOT);
		setHostName(hostName);
	}

	private void setHostName(String hostName) {
		this.hostName = hostName;
		if (IPV4_ADDRESS_PATTERN.matcher(hostName).matches()) {
			type = Type.IPv4;
		} else if (IPV6_ADDRESS_PATTERN.matcher(hostName).matches()) {
			type = Type.IPv6;
		} else {
			type = Type.hostname;
			revHost = reverseHost(hostName);
			registrySuffix = null;
			domainName = null;
			// TODO: set registry suffix and non-private domain after
			//       fix of https://github.com/crawler-commons/crawler-commons/issues/185
			EffectiveTLD privateTld = EffectiveTldFinder.getEffectiveTLD(hostName);
			if (privateTld != null) {
				privateSuffix = privateTld.getDomain();
				privateDomain = EffectiveTldFinder.getAssignedDomain(hostName, true);
			}
		}
	}

	public String getRegistrySuffix() {
		return registrySuffix;
	}

	public String getDomainNameUnderRegistrySuffix() {
		return domainName;
	}

	public String getPrivateSuffix() {
		return privateSuffix;
	}

	public String getPrivateDomainName() {
		return privateDomain;
	}

	public String[] getReverseHost() {
		return revHost;
	}

	/**
	 * Split host name into parts in reverse order: www.example.com becomes [com,
	 * example, www].
	 * 
	 * @param hostName
	 * @return parts of hostname in reverse order
	 */
	public static String[] reverseHost(String hostName) {
		String[] rev = SPLIT_HOST_PATTERN.split(hostName);
		for (int i = 0; i < (rev.length/2); i++) {
			String temp = rev[i];
			rev[i] = rev[rev.length - i - 1];
			rev[rev.length - i - 1] = temp;
		}
		return rev;
	}

	/**
	 * Create {@link Row} representing a host (data type &quot;string&quot; if not
	 * otherwise specified):
	 * <ol>
	 * <li>host name
	 * <li>reverse host (array of strings: [com, example, www])
	 * <li>registry suffix
	 * <li>domain name below registry suffix
	 * <li>private suffix
	 * <li>domain name below private suffix
	 * </ol>
	 * Reverse host is null if the host name is an IP address. Domain name and
	 * suffixes are null if the host name is an IP address, or if no valid suffix is
	 * found.
	 * 
	 * @return row
	 */
	public Row asRow() {
		return RowFactory.create(
				hostName,
				revHost,
				getRegistrySuffix(),
				getDomainNameUnderRegistrySuffix(),
				getPrivateSuffix(),
				getPrivateDomainName()
				);
	}

}