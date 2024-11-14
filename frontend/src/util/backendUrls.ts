export default {
  getInfo() {
    return '/api/info';
  },

  getVolunteers() {
    return '/api/volunteers';
  },

  patchVolunteer(id: number) {
    return `/api/volunteers/${id}`;
  },

  eventStream() {
    return '/api/balloons';
  },

  login() {
    return '/api/login';
  },

  register() {
    return '/api/register';
  },
};
