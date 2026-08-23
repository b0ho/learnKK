/** Central route path constants. */
export const PATHS = {
  login: '/login',
  signup: '/signup',
  meetings: '/meetings',
  meetingCreate: '/meetings/new',
  meetingDetail: (id: number | string) => `/meetings/${id}`,
  meetingContent: (id: number | string) => `/meetings/${id}/content`,
  adminApproval: '/admin/meetings',
  myLearning: '/my-learning',
  profile: '/me',
} as const;
