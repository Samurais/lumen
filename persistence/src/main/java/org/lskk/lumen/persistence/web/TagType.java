package org.lskk.lumen.persistence.web;

/**
 * Used by {@link TagLabel}.
 * @author ceefour
 */
public enum TagType {
	NONE,
	SPAN,
	/**
	 * Use this for formal IDs such as SKU.
	 */
	KBD,
	/**
	 * Use this for actual machine IDs.
	 */
	CODE
}
