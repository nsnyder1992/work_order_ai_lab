<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>About - WorkOrderAI</title>
    <!-- Bootstrap 5 CDN -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
        <div class="container-fluid">
            <a class="navbar-brand" href="/work-ai/about.jsp">Work Order App</a>
            <div class="d-flex">
                <a href="/work-ai/login" class="btn btn-outline-light">Login</a>
            </div>
        </div>
    </nav>
    
    <div class="container py-5">
        <h1 class="mb-4 text-primary">About WorkOrderAI</h1>

        <!-- Mission Section -->
        <div class="card mb-5 shadow-sm">
            <div class="card-body">
                <h2 class="card-title h4">Our Mission</h2>
                <p class="card-text">
                    WorkOrderAI is an intelligent platform designed to streamline work order management by integrating the power of AI with a user-friendly web application.
                    We aim to reduce downtime, improve troubleshooting accuracy, and simplify communication between field techs, customers, and support staff.
                </p>
                <p class="card-text">
                    Our AI assistant helps users understand and resolve issues faster by leveraging data from previous work orders, including repair suggestions, cost estimates, and resolution strategies.
                </p>
            </div>
        </div>

        <!-- Team Members -->
        <div class="mb-5">
            <h2 class="mb-3">Meet the Team</h2>
            <div class="row g-4">
                <!-- John Smith -->
                <div class="col-md-6">
                    <div class="card h-100 shadow-sm">
                        <div class="card-body">
                            <h5 class="card-title text-dark">John Smith</h5>
                            <h6 class="card-subtitle mb-2 text-muted">Founder & Senior Software Engineer</h6>
                            <p class="card-text">
                                John is the visionary behind WorkOrderAI. With over a decade of experience in software development, backend systems, and scalable architectures, John has led multiple enterprise projects in the IoT and automation sectors. He founded WorkOrderAI to bridge the gap between human knowledge and AI in the maintenance and repair domain.
                            </p>
                            <p class="card-text">He began tinkering with systems shortly after graduating high school in the late 1988, and hasn't stopped since</p>
                            <p class="card-text"><strong>Hobbies:</strong> Spending time with his wife Jennifer and their dog Skip, flying drones, and mountain biking.</p>
                        </div>
                    </div>
                </div>
                <!-- Emily Nguyen -->
                <div class="col-md-6">
                    <div class="card h-100 shadow-sm">
                        <div class="card-body">
                            <h5 class="card-title text-dark">Emily Nguyen</h5>
                            <h6 class="card-subtitle mb-2 text-muted">Junior Software Engineer</h6>
                            <p class="card-text">
                                Emily recently joined the team and contributes to frontend features, bug fixes, and testing infrastructure. She brings fresh ideas, curiosity, and a strong passion for ethical AI development.
                            </p>
                            <p class="card-text"><strong>Fun fact:</strong> Emily originally studied graphic design before pivoting to software engineering.</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- App Updates -->
        <div class="card shadow-sm">
            <div class="card-body">
                <h2 class="card-title h4">App Updates</h2>
                <ul class="list-group list-group-flush">
                    <li class="list-group-item"><strong>June 2025:</strong> Backend updated to encrypt any passwords found inside work orders so the AI can be trained on them securely.</li>
                    <li class="list-group-item"><strong>May 2025:</strong> Added AI chatbot with memory of previous user interactions.</li>
                    <li class="list-group-item"><strong>April 2025:</strong> File upload support added for images, videos, and PDFs.</li>
                    <li class="list-group-item"><strong>March 2025:</strong> Admin panel launched with full search and work order filtering.</li>
                </ul>
            </div>
        </div>

    </div>

    <!-- Bootstrap JS (optional) -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
