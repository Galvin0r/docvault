export const RESEND_KEY = "lastResendTimestamp";
export const RESEND_COOLDOWN_SECONDS = 60;

export interface Term {
  code: string;
  name: string;
}

export const httpErrors = [
  {
    code: 'IC',
    name: 'Incorrect username/email or password.',
  },
  {
    code: 'NAA',
    name: 'Account is not activated.',
  },
  {
    code: 'DE',
    name: 'User with this email already exists.',
  },
  {
    code: 'DU',
    name: 'User with this username already exists.',
  },
  {
    code: 'ATE',
    name: 'Activation token has expired.',
  },
  {
    code: 'ATI',
    name: 'Invalid activation token.',
  },
  {
    code: 'PRTI',
    name: 'Password reset token is invalid.',
  },
  {
    code: 'PRTE',
    name: 'Password reset token has expired.',
  },
  {
    code: 'UNF',
    name: 'User not found.',
  }
];

export type HttpErrorCode = (typeof httpErrors)[number]['code'];