package bioc.esViews.BioCDocument;

import bioc.esViews.BioCDocument.BioCDocument__BioCLocation;

import java.util.*;
import org.springframework.data.annotation.*;
import org.springframework.data.elasticsearch.annotations.*;
import static org.springframework.data.elasticsearch.annotations.FieldIndex.*;

import lombok.Data;

/**
 * Stand off annotation. The connection to the original text can be made through the {@code location} and the {@code text} fields.
*/
@Data
public class BioCDocument__BioCAnnotation {
	/**
	 * Id used to identify this annotation in a {@link Relation}.
	*/
	private String id;

	/**
	 * The annotated text.
	*/
	private String text;

	private Map<String, String>  infons;

	@Field(type = FieldType.Nested)
	private List<BioCDocument__BioCLocation> locations = new ArrayList<BioCDocument__BioCLocation>();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public BioCDocument__BioCAnnotation() {}
	public BioCDocument__BioCAnnotation(String id, String text, Map<String, String>  infons, List<BioCDocument__BioCLocation> locations) {
		this.id = id;
		this.text = text;
		this.infons = infons;
		this.locations = locations;
	}


}
