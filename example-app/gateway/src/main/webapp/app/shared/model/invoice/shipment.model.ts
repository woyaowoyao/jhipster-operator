import { Moment } from 'moment';

export interface IShipment {
  id?: number;
  trackingCode?: string;
  date?: Moment;
  details?: string;
  invoiceCode?: string;
  invoiceId?: number;
}

export const defaultValue: Readonly<IShipment> = {};
