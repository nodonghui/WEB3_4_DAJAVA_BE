package com.dajava.backend.domain.heatmap.image.service.pageCapture;

public interface FileCleanupService {

	void deleteFile(String fileName);

	void deleteNonLinkedFile();
}
