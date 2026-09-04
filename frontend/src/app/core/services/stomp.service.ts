import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { WS_BASE_URL } from './api.config';

type Handler = (payload: unknown) => void;

@Injectable({ providedIn: 'root' })
export class StompService {
  private socket?: WebSocket;
  private connected = false;
  private handlers = new Map<string, Handler[]>();
  private pending: string[] = [];
  readonly connectionState = new Subject<boolean>();

  constructor(private readonly auth: AuthService) {}

  connect(): void {
    if (this.socket && (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)) return;
    this.socket = new WebSocket(WS_BASE_URL);
    this.socket.onopen = () => {
      this.sendFrame('CONNECT', {
        Authorization: `Bearer ${this.auth.token() ?? ''}`,
        'accept-version': '1.2',
        'heart-beat': '0,0'
      });
    };
    this.socket.onmessage = (event) => this.handleFrame(String(event.data));
    this.socket.onclose = () => { this.connected = false; this.connectionState.next(false); };
  }

  subscribe(destination: string, handler: Handler): void {
    const handlers = this.handlers.get(destination) ?? [];
    handlers.push(handler);
    this.handlers.set(destination, handlers);
    const frame = this.buildFrame('SUBSCRIBE', { id: `sub-${destination}`, destination }, '');
    if (this.connected) this.socket?.send(frame);
    else this.pending.push(frame);
  }

  unsubscribe(destination: string): void {
    this.handlers.delete(destination);
    if (this.connected) {
      this.socket?.send(this.buildFrame('UNSUBSCRIBE', { id: `sub-${destination}`, destination }, ''));
    }
  }

  send(destination: string, payload: unknown = {}): void {
    const frame = this.buildFrame('SEND', { destination, 'content-type': 'application/json' }, JSON.stringify(payload));
    if (this.connected) this.socket?.send(frame);
    else this.pending.push(frame);
  }

  disconnect(): void {
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.sendFrame('DISCONNECT', {});
      this.socket.close();
    }
    this.handlers.clear();
    this.pending = [];
  }

  private handleFrame(raw: string): void {
    for (const frame of raw.split('\0').filter(Boolean)) {
      const [head, body = ''] = frame.split('\n\n');
      const lines = head.split('\n');
      const command = lines.shift();
      const headers = new Map<string, string>();
      lines.forEach((line) => {
        const index = line.indexOf(':');
        if (index > -1) headers.set(line.slice(0, index), line.slice(index + 1));
      });
      if (command === 'CONNECTED') {
        this.connected = true;
        this.connectionState.next(true);
        this.pending.splice(0).forEach((pendingFrame) => this.socket?.send(pendingFrame));
        return;
      }
      if (command === 'MESSAGE') {
        const destination = headers.get('destination') ?? '';
        const payload = body ? JSON.parse(body) : null;
        (this.handlers.get(destination) ?? []).forEach((handler) => handler(payload));
      }
    }
  }

  private sendFrame(command: string, headers: Record<string, string>, body = ''): void {
    this.socket?.send(this.buildFrame(command, headers, body));
  }

  private buildFrame(command: string, headers: Record<string, string>, body: string): string {
    const headerLines = Object.entries(headers).map(([key, value]) => `${key}:${value}`).join('\n');
    return `${command}\n${headerLines}\n\n${body}\0`;
  }
}
