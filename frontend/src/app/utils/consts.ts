export const RESEND_KEY = "lastResendTimestamp";
export const RESEND_COOLDOWN_SECONDS = 60;

export interface Term {
  code: string;
  name: string;
}

export const httpErrors = [
  {
    code: 'auth.bad_credentials',
    name: 'Incorrect username/email or password.',
  },
  {
    code: 'user.not_activated',
    name: 'Account is not activated.',
  },
  {
    code: 'user.email_taken',
    name: 'User with this email already exists.',
  },
  {
    code: 'user.login_taken',
    name: 'User with this username already exists.',
  },
  {
    code: 'auth.activation_token.expired',
    name: 'Activation token has expired.',
  },
  {
    code: 'auth.activation_token.invalid',
    name: 'Invalid activation token.',
  },
  {
    code: 'auth.password_reset_token.invalid',
    name: 'Password reset token is invalid.',
  },
  {
    code: 'auth.password_reset_token.expired',
    name: 'Password reset token has expired.',
  },
  {
    code: 'user.not_found',
    name: 'User not found.',
  }
];

export type HttpErrorCode = (typeof httpErrors)[number]['code'];

export const DEBOUNCE_MS = 250;