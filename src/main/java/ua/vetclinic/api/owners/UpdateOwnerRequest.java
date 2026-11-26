package ua.vetclinic.api.owners;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ua.vetclinic.api.common.ErrorDetail;
import ua.vetclinic.api.common.Strings;

/**
 * PATCH: сеттери запам'ятовують, які поля прийшли, щоб відрізнити відсутнє поле від явного null.
 */
public class UpdateOwnerRequest {

  private final Set<String> provided = new HashSet<>();

  @Size(min = 1, max = 150)
  private String fullName;

  @Pattern(regexp = "^\\+?[0-9]{10,15}$")
  private String phone;

  @Email
  @Pattern(regexp = ".+")
  @Size(max = 254)
  private String email;

  @Size(min = 1, max = 300)
  private String address;

  /**
   * Приймає ПІБ з JSON і позначає поле переданим.
   *
   * @param fullName нове ПІБ; {@code null} відхиляється як обов'язкове поле
   */
  public void setFullName(String fullName) {
    this.fullName = Strings.strip(fullName);
    provided.add("fullName");
  }

  /**
   * Приймає телефон з JSON і позначає поле переданим.
   *
   * @param phone новий телефон; {@code null} відхиляється як обов'язкове поле
   */
  public void setPhone(String phone) {
    this.phone = Strings.strip(phone);
    provided.add("phone");
  }

  /**
   * Приймає email з JSON і позначає поле переданим.
   *
   * @param email новий email або {@code null}, щоб його очистити
   */
  public void setEmail(String email) {
    this.email = Strings.strip(email);
    provided.add("email");
  }

  /**
   * Приймає адресу з JSON і позначає поле переданим.
   *
   * @param address нова адреса або {@code null}, щоб її очистити
   */
  public void setAddress(String address) {
    this.address = Strings.strip(address);
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

  List<ErrorDetail> nullRequiredFields() {
    List<ErrorDetail> details = new ArrayList<>();
    if (isProvided("fullName") && fullName == null) {
      details.add(new ErrorDetail("fullName", "REQUIRED", "Поле обов'язкове"));
    }
    if (isProvided("phone") && phone == null) {
      details.add(new ErrorDetail("phone", "REQUIRED", "Поле обов'язкове"));
    }
    return details;
  }
}
