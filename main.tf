provider "aws" {
  region = "eu-west-1"
}

# Step 1: Check if the IAM role already exists
data "aws_iam_role" "existing_role" {
  name = "step_function_execution_role"
}

# Step 2: Create the IAM role if it doesn't exist
resource "aws_iam_role" "step_function_role" {
  count = length(data.aws_iam_role.existing_role.id) > 0 ? 0 : 1  # Only create the role if it doesn't exist

  name = "step_function_execution_role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action    = "sts:AssumeRole"
        Effect    = "Allow"
        Principal = {
          Service = "states.amazonaws.com"
        }
      }
    ]
  })
}

# Step 3: Attach policies to the role
resource "aws_iam_role_policy" "step_function_policy" {
  count  = length(data.aws_iam_role.existing_role.id) > 0 ? 0 : 1  # Only create policy if role is created
  name   = "step_function_execution_policy"
  role   = aws_iam_role.step_function_role[0].id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action    = "lambda:InvokeFunction"
        Effect    = "Allow"
        Resource  = "*"
      }
    ]
  })
}
