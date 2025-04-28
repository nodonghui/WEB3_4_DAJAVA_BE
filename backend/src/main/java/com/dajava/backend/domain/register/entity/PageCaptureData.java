package com.dajava.backend.domain.register.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class PageCaptureData {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String captureFileName;

	@Column(nullable = false)
	private String pageUrl;

	// 각 너비를 나누는 시작점 (100px 단위)
	@Column(nullable = false)
	private int widthRange;

	@ManyToOne
	@JoinColumn(name = "register_id", nullable = false)
	private Register register;

	public void updateCaptureFileName(String captureFileName) {
		this.captureFileName = captureFileName;
	}
}
