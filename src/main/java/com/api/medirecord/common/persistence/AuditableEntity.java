package com.api.medirecord.common.persistence;

import java.time.Instant;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

/**
 * Base de todas las entidades: trazabilidad de creacion y modificacion.
 *
 * <p>El usuario auditor lo resuelve {@code JpaAuditingConfig} a partir del JWT
 * (Keycloak); sin sesion autenticada se registra como {@code system}.</p>
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@CreatedBy
	@Column(name = "created_by", length = 100, updatable = false)
	private String createdBy;

	@LastModifiedDate
	@Column(name = "updated_at")
	private Instant updatedAt;

	@LastModifiedBy
	@Column(name = "updated_by", length = 100)
	private String updatedBy;

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

}