import 'tek-ms-ds/dist/style.css';
import { ClientBar, type ClientbarInterface, type Customer } from './clientBar';

const app = document.querySelector<HTMLDivElement>("#app")!;

const clientBar = new ClientBar(app);



const mockCustomers: Customer[] = [
  { firstname: 'Jean', lastname: 'Dupont', phone: '06 000 00 00' },
  { firstname: 'Marie', lastname: 'Curie', phone: '06 000 00 00' },
];

const mockClientBar: ClientbarInterface = {
  customers: mockCustomers,
  callback: (customer: Customer) => {
    console.log('Callback déclenché. Nouveaux clients :', customer);
  },
  selectedCustomer: (customer: Customer) => {
    console.log(customer);

  }
};


clientBar.render(mockClientBar);
