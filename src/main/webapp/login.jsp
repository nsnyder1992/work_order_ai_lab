<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<!DOCTYPE html>
<html lang="en">
<head>
<title>Login</title>
<link
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css"
	rel="stylesheet">
</head>
<body>
	<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
		<div class="container-fluid">
			<a class="navbar-brand" href="/work-ai/about.jsp">Work Order App</a>
		</div>
	</nav>

	<div class="d-flex justify-content-center align-items-center vh-100">

		<div class="card p-4" style="width: 350px;">
			<h3 class="mb-3 text-center">Login</h3>
			<form method="post" action="login">
				<div class="mb-3">
					<label for="username" class="form-label">Username</label> <input
						type="text" name="username" id="username" class="form-control"
						required autofocus>
				</div>
				<div class="mb-3">
					<label for="password" class="form-label">Password</label> <input
						type="password" name="password" id="password" class="form-control"
						required>
				</div>
				<c:if test="${not empty error}">
					<div class="alert alert-danger">${error}</div>
				</c:if>
				<button type="submit" class="btn btn-primary w-100">Login</button>
			</form>
			<div class="mt-3 text-center">
				Don't have an account? <a href="signup.jsp">Sign Up</a>
			</div>
		</div>
	</div>
</body>
</html>