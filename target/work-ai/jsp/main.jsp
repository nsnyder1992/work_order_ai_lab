<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
if (session.getAttribute("user") == null) {
	response.sendRedirect("/work-ai/login.jsp");
	return;
}

String success = request.getParameter("success");

if (request.getAttribute("workOrders") == null) {
    response.sendRedirect("/work-ai/jsp/main");
}
%>
<!DOCTYPE html>
<html lang="en">
<head>
<title>Dashboard - Work Order App</title>
<link
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css"
	rel="stylesheet">
<style>
.chatbox {
	height: 500px;
	overflow-y: auto;
	background-color: #f8f9fa;
	border: 1px solid #ccc;
	padding: 1rem;
}
#typing-indicator {
	font-style: italic;
	color: #555;
	margin-top: 8px;
}
</style>
</head>
<body>
	<script>
	
		function toggleTab(tabId) {
		    console.log("Toggling tab: " + tabId);
		    switch(tabId) {
		        case 'create-tab':
		            showTab('create');
		            hideTab('orders');
		            break;
		        case 'orders-tab':
		            showTab('orders');
		            hideTab('create');
		            break;
		        default:
		    }
		}
		
		function showTab(tabId) {
			const tab = document.querySelector(`#` + tabId + "-tab");
		    const tabContent = document.querySelector(`#` + tabId);
		    if (tab) {
		    	tab.classList.add('active');
		    }
		    if (tabContent) {
		        tabContent.classList.add('show', 'active');
		    }
		}
		
		function hideTab(tabId) {
            const tab = document.querySelector(`#` + tabId + "-tab");
            const tabContent = document.querySelector(`#` + tabId);
            if (tab) {
                tab.classList.remove('active');
            }
		    if (tabContent) {
		        tabContent.classList.remove('show', 'active');
		    }
		}
		
		//Allowed file extensions
		const allowedExtensions = ['jpg', 'jpeg', 'gif', 'png', 'pdf', 'csv', 'txt', 'xlsm', 'xlsx'];
		
		// Function to validate file extensions
		function validateFileInput(inputId) {
		    console.log("Validating file input: " + inputId);
		    const fileInput = document.getElementById(inputId);
		    const files = fileInput.files;
		    const errorMessage = document.getElementById(inputId + '-error');
		    errorMessage.innerHTML = ''; // Clear previous error messages
		
		    for (let i = 0; i < files.length; i++) {
		        const fileName = files[i].name;
		        const fileExtension = fileName.split('.').pop().toLowerCase();
		
		        if (!allowedExtensions.includes(fileExtension)) {
		            errorMessage.innerHTML = "Invalid file type: " + fileName + ". Allowed types are: " + allowedExtensions.join(', ')+ ".";
		            fileInput.value = ''; // Clear the invalid file input
		            return false;
		        }
		    }
		    return true;
		}   
	
	</script>
	<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
		<div class="container-fluid">
			<a class="navbar-brand" href="/work-ai/about.jsp">Work Order App</a>
			<div class="d-flex">
				<a href="/work-ai/logout" class="btn btn-outline-light">Logout</a>
			</div>
		</div>
	</nav>


	<!-- Success Banner -->
	<c:if test="${param.success == 'true'}">
		<div id="success"
			class="alert alert-success alert-dismissible fade show" role="alert">
			Work order created successfully!
			<button type="button" class="btn-close" data-bs-dismiss="alert"
				aria-label="Close" onClick="hideTab('success')"></button>
		</div>
	</c:if>

	<!-- Rest of the page content -->
	<!-- Your existing code here -->

	<div class="container-fluid mt-4">
		<div class="row">
			<!-- Left: Work Orders -->
			<div class="col-md-6">
				<div class="container-fluid mt-4">
					<ul class="nav nav-tabs" id="workOrderTabs" role="tablist">
						<li class="nav-item" role="presentation">
							<button class="nav-link active" id="create-tab"
								data-bs-toggle="tab" data-bs-target="#create" type="button"
								role="tab" aria-controls="create" aria-selected="true"
								onClick="toggleTab('create-tab')">Create Work Order</button>
						</li>
						<li class="nav-item" role="presentation">
							<button class="nav-link" id="orders-tab" data-bs-toggle="tab"
								data-bs-target="#orders" type="button" role="tab"
								aria-controls="orders" aria-selected="false"
								onClick="toggleTab('orders-tab')">Your Work Orders</button>
						</li>
					</ul>
					<div class="tab-content" id="workOrderTabsContent">
						<!-- Create Work Order Tab -->
						<div class="tab-pane fade show active" id="create" role="tabpanel"
							aria-labelledby="create-tab">
							<h4 class="mt-3">Create Work Order</h4>
							<form method="post" action="main" enctype="multipart/form-data">
								<div class="mb-3">
									<label for="title" class="form-label">Title</label> <input
										name="title" id="title" class="form-control" required />
								</div>
								<div class="mb-3">
									<label for="description" class="form-label">Description</label>
									<textarea name="description" id="description"
										class="form-control" rows="3" required></textarea>
								</div>
								<div class="mb-3">
									<select class="form-control" name="status">
										<option value="in progress">In Progress</option>
										<option value="complete">Complete</option>
									</select>
								</div>
								<div class="mb-3">
									<label for="solution" class="form-label">Solution</label>
									<textarea name="solution" id="solution" class="form-control"
										rows="2"></textarea>
								</div>
								<div class="mb-3">
									<label for="quote" class="form-label">Quote ($)</label> <input
										type="number" name="quote" id="quote" class="form-control"
										min="0" />
								</div>
								<div class="mb-3">
									<label for="finalCost" class="form-label">Final Cost
										($)</label> <input type="number" name="finalCost" id="finalCost"
										class="form-control" min="0" />
								</div>
								<div class="mb-3">
									<label for="uploadedFiles" class="form-label">Upload
										Files</label> <input type="file" name="uploadedFiles" 
										id="uploadedFiles" class="form-control" multiple
										 onchange="validateFileInput('uploadedFiles')" />
										<div id="uploadedFiles-error" class="text-danger"></div>
								</div>
								<!-- Admin Uploads -->
						       <c:if test="${isAdmin}">
						       <div class="mb-3">
                                    <label for="complianceFiles" class="form-label">Compliance
                                        Files</label> <input type="file" name="complianceFiles"
                                        id="complianceFiles" class="form-control" multiple 
                                        onchange="validateFileInput('complianceFiles')" />
                                        <div id="uploadedFiles-error" class="text-danger"></div>
                                        </div>
						       </c:if>
								<button type="submit" class="btn btn-primary">Create</button>
							</form>
						</div>

						<!-- Your Work Orders Tab -->
						<div class="tab-pane fade" id="orders" role="tabpanel"
							aria-labelledby="orders-tab">
							<h4 class="mt-3">Your Work Orders</h4>
							<c:forEach var="wo" items="${workOrders}">
								<div class="card mb-2">
									<div class="card-body">
										<h5 class="card-title"><c:out value="#${wo.number} - ${wo.title}" /></h5>
										<p class="card-text"><c:out value="${wo.description}" /></p>
										<span class="badge bg-secondary"><c:out value="#${wo.status}" /></span>
										<div class="text-muted mt-1" style="font-size: 0.85rem;">Created:
											<c:out value="${wo.createdAt}" /></div>
										<hr />
										<p>
											<strong>Solution:</strong> <c:out value="${wo.solution}" />
										</p>
										<p>
											<strong>Quote:</strong> <c:out value="$${wo.quote}" />
										</p>
										<p>
											<strong>Final Cost:</strong> <c:out value="$${wo.finalCost}" />
										</p>
									</div>
								</div>
							</c:forEach>
						</div>
					</div>
				</div>
			</div>

			<!-- Right: AI Assistant -->
			<div class="col-md-6">
				<h4>AI Assistant</h4>
				<div class="chatbox">
					<div id="chatLog"></div>
					<div id="typing-indicator" style="display: none;">
						<em>AI is thinking...</em>
					</div>
				</div>
				<form id="chatForm" class="mt-3" action="chat" method="post">
					<div class="input-group">
						<input type="textarea" class="form-control" id="userPrompt"
							placeholder="Ask the Work Order AI something..." required>
						<button type="submit" class="btn btn-primary">Send</button>
					</div>
				</form>
			</div>
		</div>
	</div>
	</div>


	<script>
	
	window.onload = function() {
	    
	    document.getElementById('chatForm').addEventListener('submit', async function(e) {
	        e.preventDefault();
	        typingIndicator.style.display = "block";
	        const prompt = document.getElementById('userPrompt').value;
	        const chatLog = document.getElementById('chatLog');

	        chatLog.innerHTML += `<p><strong>You:</strong> ` + prompt + `</p>`;

	        const res = await fetch('chat', {
	            method: 'POST',
	            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
	            body: "question=" + encodeURIComponent(prompt)
	        });

	        const text = await res.text();
	        typingIndicator.style.display = "none";
	        chatLog.innerHTML += `<p><strong>AI:</strong> ` + text + `</p>`;
	        document.getElementById('userPrompt').value = '';
	    });

	    const form = document.getElementById("chat-form");
	    const typingIndicator = document.getElementById("typing-indicator");
    };
	


</script>

</body>
</html>