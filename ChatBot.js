// chatbot.js

const questions = {
  Technical: [
    "Explain a challenging technical problem you solved.",
    "Describe the architecture of a project you built.",
    "How do you approach debugging a complex issue?",
    "Which data structures would you use for X (explain why)?",
    "How do you ensure code quality and testing?"
  ],
  HR: [
    "Tell me about yourself and your background.",
    "Why do you want to work for our company?",
    "What are your strengths and weaknesses?",
    "Tell us about a time you received constructive criticism.",
    "Where do you see yourself in 5 years?"
  ],
  Interpersonal: [
    "Describe a conflict you had with a coworker and how you resolved it.",
    "Tell me about a time you led a team under pressure.",
    "Give an example of when you had to persuade someone to accept your idea.",
    "How do you handle feedback that you disagree with?",
    "Tell me about a time you helped a colleague improve."
  ]
};

let currentCategory = '';
let currentQuestionIndex = 0;
let currentAnswers = [];

function startInterview() {
  currentCategory = document.getElementById("category").value;
  currentQuestionIndex = 0;
  currentAnswers = [];
  document.getElementById("questionSection").style.display = 'block';
  document.getElementById("feedbackSection").style.display = 'none';
  showQuestion();
}

function showQuestion() {
  const question = questions[currentCategory][currentQuestionIndex];
  document.getElementById("questionText").innerText = `Q${currentQuestionIndex + 1}: ${question}`;
  document.getElementById("answerInput").value = '';
}

function submitAnswer() {
  const answer = document.getElementById("answerInput").value.trim();
  if (!answer) {
    alert("Please type an answer.");
    return;
  }

  currentAnswers.push(answer);

  // Simulate feedback (you can replace this with an API call)
  const feedback = generateLocalFeedback(answer);
  document.getElementById("feedbackText").innerText = feedback;

  document.getElementById("feedbackSection").style.display = 'block';
}

function nextQuestion() {
  currentQuestionIndex++;
  if (currentQuestionIndex >= 5) {
    alert("Practice session complete! Refresh to try another.");
    return;
  }

  document.getElementById("feedbackSection").style.display = 'none';
  showQuestion();
}

function generateLocalFeedback(answer) {
  if (answer.length < 40) {
    return `CRITIQUE:\n- Answer is too short or lacks detail.\nSUGGESTIONS:\n- Use the STAR method.\n- Add measurable results.\nTIPS:\n- Speak with confidence.`;
  }
  return `CRITIQUE:\n- Decent structure and some detail.\nSUGGESTIONS:\n- Include outcomes and metrics.\n- Show more depth or challenges.\nTIPS:\n- Time yourself to stay concise.`;
}
