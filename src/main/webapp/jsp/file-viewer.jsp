<%@ page contentType="text/html;charset=UTF-8" language="java" %>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
        <!DOCTYPE html>
        <html lang="en">

        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>File Viewer -
                <c:out value="${fileName}" />
            </title>
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
            <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.7.2/font/bootstrap-icons.css" rel="stylesheet">
            <style>
                body {
                    padding: 20px;
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                }

                .file-header {
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                    padding: 20px;
                    border-radius: 10px;
                    margin-bottom: 20px;
                }

                .compliance-header {
                    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
                }

                .file-content {
                    border: 1px solid #dee2e6;
                    border-radius: 10px;
                    padding: 20px;
                    background-color: #f8f9fa;
                    min-height: 400px;
                }

                .text-preview {
                    background: white;
                    border: 1px solid #ddd;
                    border-radius: 5px;
                    padding: 15px;
                    font-family: 'Courier New', monospace;
                    font-size: 14px;
                    white-space: pre-wrap;
                    max-height: 500px;
                    overflow-y: auto;
                }

                .download-section {
                    margin-top: 20px;
                    padding: 15px;
                    background: #e3f2fd;
                    border-radius: 10px;
                    border-left: 4px solid #2196f3;
                }

                .file-info {
                    background: white;
                    border-radius: 10px;
                    padding: 15px;
                    margin-bottom: 20px;
                    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
                }

                .error-container {
                    text-align: center;
                    padding: 40px;
                }

                .file-icon {
                    font-size: 4rem;
                    color: #6c757d;
                    margin-bottom: 20px;
                }

                .zip-content {
                    background: white;
                    border: 1px solid #ddd;
                    border-radius: 5px;
                    max-height: 500px;
                    overflow-y: auto;
                }

                .zip-file-item {
                    padding: 10px 15px;
                    border-bottom: 1px solid #eee;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                }

                .zip-file-item:last-child {
                    border-bottom: none;
                }

                .zip-file-item:hover {
                    background-color: #f8f9fa;
                }

                .zip-file-info {
                    flex-grow: 1;
                }

                .zip-file-name {
                    font-weight: 500;
                    margin-bottom: 2px;
                }

                .zip-file-size {
                    font-size: 0.85em;
                    color: #6c757d;
                }

                .zip-loading {
                    text-align: center;
                    padding: 40px;
                    color: #6c757d;
                }

                .zip-error {
                    text-align: center;
                    padding: 20px;
                    color: #dc3545;
                    background-color: #f8d7da;
                    border: 1px solid #f5c6cb;
                    border-radius: 5px;
                    margin: 10px 0;
                }
            </style>
        </head>

        <body>
            <c:choose>
                <c:when test="${not empty error}">
                    <div class="error-container">
                        <div class="alert alert-danger" role="alert">
                            <i class="bi bi-exclamation-triangle-fill"></i>
                            <h4>Error</h4>
                            <p>
                                <c:out value="${error}" />
                            </p>
                        </div>
                        <button type="button" class="btn btn-secondary" onclick="window.close()">
                            <i class="bi bi-x-circle"></i> Close
                        </button>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="file-header <c:if test='${docType eq " compliance"}'>compliance-header</c:if>">
                        <div class="d-flex justify-content-between align-items-start">
                            <div>
                                <h3 class="mb-1">
                                    <i class="bi <c:choose>
                            <c:when test='${docType eq " compliance"}'>bi-shield-check</c:when>
                                        <c:when test='${contentType.startsWith("image/")}'>bi-image</c:when>
                                        <c:when test='${contentType eq "application/pdf"}'>bi-file-earmark-pdf</c:when>
                                        <c:when test='${contentType.startsWith("text/")}'>bi-file-earmark-text</c:when>
                                        <c:otherwise>bi-file-earmark</c:otherwise>
            </c:choose>"></i>
            <c:out value="${fileName}" />
            </h3>
            <p class="mb-1">Work Order:
                <c:out value="${workOrderNumber} - ${workOrderTitle}" />
            </p>
            <c:if test='${docType eq "compliance"}'>
                <span class="badge bg-warning">
                    <i class="bi bi-shield-check"></i> Compliance Document
                </span>
            </c:if>
            </div>
            <button type="button" class="btn btn-light btn-sm" onclick="window.close()">
                <i class="bi bi-x-lg"></i>
            </button>
            </div>
            </div>

            <div class="file-info">
                <div class="row">
                    <div class="col-md-6">
                        <small class="text-muted">File Type:</small><br>
                        <span class="fw-bold">
                            <c:out value="${contentType}" />
                        </span>
                    </div>
                    <div class="col-md-6">
                        <small class="text-muted">File Size:</small><br>
                        <span class="fw-bold">
                            <c:choose>
                                <c:when test="${fileSize < 1024}">
                                    ${fileSize} bytes
                                </c:when>
                                <c:when test="${fileSize < 1024 * 1024}">
                                    ${Math.round(fileSize / 1024 * 100) / 100} KB
                                </c:when>
                                <c:otherwise>
                                    ${Math.round(fileSize / (1024 * 1024) * 100) / 100} MB
                                </c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                </div>
            </div>

            <div class="file-content">
                <c:choose>
                    <c:when test="${canPreview}">
                        <c:choose>
                            <c:when test="${contentType.startsWith('image/')}">
                                <div class="text-center">
                                    <img src="${imageData}" class="img-fluid"
                                        style="max-height: 600px; border-radius: 10px;" alt="${fileName}">
                                </div>
                            </c:when>

                            <c:when test="${contentType.startsWith('text/')}">
                                <h5><i class="bi bi-file-earmark-text"></i> Text Preview</h5>
                                <div class="text-preview">
                                    <c:out value="${textContent}" />
                                </div>
                            </c:when>

                            <c:when test="${contentType eq 'application/pdf'}">
                                <h5><i class="bi bi-file-earmark-pdf"></i> PDF Preview</h5>
                                <div class="text-center">
                                    <p class="text-muted mb-3">PDF files are best viewed by downloading them.</p>
                                    <iframe src="file-download?docId=${documentId}" width="100%" height="500px"
                                        style="border: 1px solid #ddd; border-radius: 5px;">
                                        <p>Your browser does not support PDF preview. Please <a
                                                href="file-download?docId=${documentId}">download the file</a>.</p>
                                    </iframe>
                                </div>
                            </c:when>

                            <c:when test="${isZipFile}">
                                <!-- ZIP files extracted to /tmp/zip_extracts -->
                                <h5><i class="bi bi-file-earmark-zip"></i> ZIP Archive Contents</h5>
                                <div id="zip-content-container">
                                    <div class="zip-loading">
                                        <div class="spinner-border" role="status">
                                            <span class="visually-hidden">Loading...</span>
                                        </div>
                                        <p class="mt-2">Extracting ZIP file contents...</p>
                                    </div>
                                </div>
                            </c:when>
                        </c:choose>
                    </c:when>
                    <c:otherwise>
                        <div class="text-center">
                            <div class="file-icon">
                                <i class="bi bi-file-earmark"></i>
                            </div>
                            <h5>Preview not available</h5>
                            <p class="text-muted">This file type cannot be previewed in the browser.</p>
                            <p class="text-muted">Please download the file to view its contents.</p>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <div class="download-section">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <h6 class="mb-1"><i class="bi bi-download"></i> Download File</h6>
                        <small class="text-muted">Download the original file to your computer</small>
                    </div>
                    <div>
                        <a href="file-download?docId=${documentId}" class="btn btn-primary">
                            <i class="bi bi-download"></i> Download
                        </a>
                    </div>
                </div>
            </div>
            </c:otherwise>
            </c:choose>

            <!-- Bootstrap JS -->
            <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>

            <c:if test="${isZipFile}">
                <script>
                    // Load ZIP contents when page loads
                    document.addEventListener('DOMContentLoaded', function () {
                        loadZipContents(<c:out value="${documentId}" />);
                    });

                    function loadZipContents(documentId) {
                        fetch('zip-extractor?docId=' + documentId)
                            .then(response => {
                                if (!response.ok) {
                                    throw new Error('Failed to extract ZIP contents');
                                }
                                return response.json();
                            })
                            .then(files => {
                                displayZipContents(files, documentId);
                            })
                            .catch(error => {
                                console.error('Error loading ZIP contents:', error);
                                document.getElementById('zip-content-container').innerHTML =
                                    '<div class="zip-error"><i class="bi bi-exclamation-triangle"></i> Error extracting ZIP contents: ' + error.message + '</div>';
                            });
                    }

                    function displayZipContents(files, documentId) {
                        const container = document.getElementById('zip-content-container');

                        if (files.length === 0) {
                            container.innerHTML = '<div class="zip-error"><i class="bi bi-info-circle"></i> This ZIP file appears to be empty.</div>';
                            return;
                        }

                        let html = '<div class="zip-content">';
                        files.forEach(file => {
                            const icon = file.isDirectory ? 'bi-folder' : getFileIcon(file.name);
                            const sizeText = file.isDirectory ? 'Folder' : formatFileSize(file.size);

                            html += '<div class="zip-file-item">';
                            html += '<div class="zip-file-info">';
                            html += '<div class="zip-file-name"><i class="bi ' + icon + '"></i> ' + escapeHtml(file.name) + '</div>';
                            html += '<div class="zip-file-size">' + sizeText + '</div>';
                            html += '</div>';

                            if (!file.isDirectory) {
                                html += '<div>';
                                html += '<a href="zip-file-download?docId=' + documentId + '&file=' + encodeURIComponent(file.relativePath) + '" class="btn btn-sm btn-outline-primary">';
                                html += '<i class="bi bi-download"></i> Download';
                                html += '</a>';
                                html += '</div>';
                            }

                            html += '</div>';
                        });
                        html += '</div>';

                        container.innerHTML = html;
                    }

                    function getFileIcon(fileName) {
                        const ext = fileName.toLowerCase().split('.').pop();
                        switch (ext) {
                            case 'pdf': return 'bi-file-earmark-pdf';
                            case 'doc':
                            case 'docx': return 'bi-file-earmark-word';
                            case 'xls':
                            case 'xlsx': return 'bi-file-earmark-excel';
                            case 'ppt':
                            case 'pptx': return 'bi-file-earmark-ppt';
                            case 'txt': return 'bi-file-earmark-text';
                            case 'jpg':
                            case 'jpeg':
                            case 'png':
                            case 'gif':
                            case 'bmp': return 'bi-file-earmark-image';
                            case 'zip':
                            case 'rar':
                            case '7z': return 'bi-file-earmark-zip';
                            default: return 'bi-file-earmark';
                        }
                    }

                    function formatFileSize(bytes) {
                        if (bytes < 1024) return bytes + ' bytes';
                        if (bytes < 1024 * 1024) return Math.round(bytes / 1024 * 100) / 100 + ' KB';
                        return Math.round(bytes / (1024 * 1024) * 100) / 100 + ' MB';
                    }

                    function escapeHtml(text) {
                        const div = document.createElement('div');
                        div.textContent = text;
                        return div.innerHTML;
                    }
                </script>
            </c:if>
        </body>

        </html>