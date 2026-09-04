import { Component, EventEmitter, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-chat-input',
  standalone: true,
  imports: [FormsModule],
  template: `
    <form class="chat-input" (ngSubmit)="submit()">
      <input name="message" [(ngModel)]="content" placeholder="Type a message..." (input)="typing.emit(true)">
      <button class="btn btn-primary" [disabled]="!content.trim()">Send</button>
    </form>
  `
})
export class ChatInputComponent {
  content = '';
  @Output() send = new EventEmitter<string>();
  @Output() typing = new EventEmitter<boolean>();

  submit(): void {
    const text = this.content.trim();
    if (!text) return;
    this.send.emit(text);
    this.content = '';
  }
}
