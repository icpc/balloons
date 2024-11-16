export default {
  common: {
    loading: 'Loading...',
    error: 'Error',
    close: 'Close',
  },
  auth: {
    login: 'Login',
    register: 'Register',
    logout: 'Logout',
    loggedInAs: 'Logged in as',
    username: 'Username',
    password: 'Password',
    confirmPassword: 'Confirm password',
    loginButton: 'Log in',
    registerButton: 'Register',
    errors: {
      invalidCredentials: 'Invalid username or password',
      loginFailed: 'Login failed. Please try again.',
      passwordTooShort: 'Password must be at least 6 characters long',
      passwordMismatch: 'Passwords do not match',
      userExists: 'User with this username already exists',
      registrationFailed: 'Registration failed. Please try again.',
    },
  },
  connection: {
    connecting: 'Connecting...',
    lost: 'Connection lost. Reconnecting...',
  },
  navigation: {
    queue: 'Queue',
    delivered: 'Delivered',
    standings: 'Standings',
    volunteers: 'Volunteers',
    access: 'Access',
  },
  halls: {
    allHalls: 'All halls',
    hall: 'Hall',
  },
  balloons: {
    title: 'Balloons',
    youCarry: 'You are carrying',
    available: 'Available',
    inProgress: 'In progress',
    actions: {
      done: 'Done',
      drop: 'Drop',
      take: 'Take',
    },
    carriedBy: 'Carried by ',
    deliveredTitle: 'Delivered Balloons',
    delivered: 'Delivered',
    deliveredBy: 'Delivered by ',
  },
  errors: {
    noAccess: 'No access',
    contactAdmin: 'Contact the administrator with your username to get access.',
    notAdmin: 'You are not an administrator.',
    pageNotFound: 'Page not found.',
  },
  standings: {
    title: 'Standings',
    place: 'Place',
    team: 'Team',
  },
  volunteers: {
    title: 'Volunteer Access Management',
    loading: 'Loading...',
    noVolunteers: 'No volunteers',
    loadError: 'Failed to load data',
    updateError: 'Failed to update access rights',
    table: {
      login: 'Username',
      access: 'Access',
      balloons: 'Balloons',
      volunteers: 'Volunteers',
      thatsYou: 'That\'s you',
      grant: 'Grant',
      revoke: 'Revoke',
    },
    rating: {
      title: 'Volunteer Rating',
      volunteer: 'Volunteer',
      delivered: 'Delivered',
      noDeliveredBalloons: 'No balloons have been delivered yet',
    },
  },
  app: {
    fetchingData: 'Fetching data from server...',
  },
};
