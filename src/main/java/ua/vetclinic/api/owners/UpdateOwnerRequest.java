package ua.vetclinic.api.owners;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** PATCH: сеттери запам'ятовують, які поля прийшли, щоб відрізнити відсутнє поле від явного null. */
public class UpdateOwnerRequest {

	private final Set<String> provided = new HashSet<>();

	@Size(min = 1, max = 150)
	private String fullName;

	@Pattern(regexp = "^\\+?[0-9]{10,15}$")
	private String phone;

	@Email
	@Size(max = 254)
	private String email;

	@Size(min = 1, max = 300)
	private String address;

	public void setFullName(String fullName) {
		this.fullName = fullName;
		provided.add("fullName");
	}

	public void setPhone(String phone) {
		this.phone = phone;
		provided.add("phone");
	}

	public void setEmail(String email) {
		this.email = email;
		provided.add("email");
	}

	public void setAddress(String address) {
		this.address = address;
		provided.add("address");
	}

	String fullName() {
		return fullName;
	}

	String phone() {
		return phone;
	}

	String email() {
		return email;
	}

	String address() {
		return address;
	}

	boolean isProvided(String field) {
		return provided.contains(field);
	}

	boolean isEmpty() {
		return provided.isEmpty();
	}
}
