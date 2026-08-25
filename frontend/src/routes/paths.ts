/** Central route path constants. */
export const PATHS = {
  login: '/login',
  signup: '/signup',
  meetings: '/meetings',
  meetingCreate: '/meetings/new',
  meetingDetail: (id: number | string) => `/meetings/${id}`,
  meetingContent: (id: number | string) => `/meetings/${id}/content`,
  meetingQuestions: (id: number | string) => `/meetings/${id}/questions-edit`,
  adminApproval: '/admin/meetings',
  adminMonitoring: '/admin/monitoring',
  myLearning: '/my-learning',
  messages: '/messages',
  messageThread: (id: number | string) => `/messages/${id}`,
  profile: '/me',
  surveyAnswer: (id: number | string) => `/meetings/${id}/survey-answer`,
  feedback: (id: number | string) => `/meetings/${id}/feedback`,
  feedbackView: (id: number | string) => `/meetings/${id}/feedback-view`,
} as const;
