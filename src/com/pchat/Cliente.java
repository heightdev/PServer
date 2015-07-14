package com.pchat;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Cliente {
	private String ipServidor;
	private int portServidor;
	private String userName;
	private SimpleDateFormat sdf;
	private Socket socket;
	private ObjectInputStream input; // Para ler do socket.
	private ObjectOutputStream output; // Para escrever no socket.

	public Cliente(String ipServidor, int portServidor, String userName) {
		this.ipServidor = ipServidor;
		this.portServidor = portServidor;
		this.userName = userName;
		this.sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	}

	public Boolean start() {
		try {
			socket = new Socket(ipServidor, portServidor);
		} catch (Exception e) {
			System.out.println("Não foi possível conectar ao servidor: " + e + ".\n");
			return false;
		}

		System.out.println("Conectado ao servidor " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + ".\n");

		try {
			input  = new ObjectInputStream(socket.getInputStream());
			output = new ObjectOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			imprimirMensagem("Erro ao criar os canais de Entrada e Saída de dados: " + e + ".\n");
			return false;
		}

		// Cria a Thread para ouvir o servidor.
		new ListenFromServer().start();

		// Envia o usuário para o servidor, como mensagem. As outras mensagens serão
		// enviadas como objeto.
		try {
			output.writeObject(userName);
		} catch (IOException e) {
			imprimirMensagem("Erro ao fazer login: " + e);
			desconectar();
			return false;
		}

		return true;
	}

	public void imprimirMensagem(String mensagem) {
		System.out.println("[" + sdf.format(new Date()) + "] " + mensagem);
	}

	void sendMessage(Mensagem msg) {
		try {
			output.writeObject(msg);
		} catch(IOException e) {
			imprimirMensagem("Erro ao escrever para o servidor: " + e + ".\n");
		}
	}

	private void desconectar() {
		try { if (input != null) input.close(); } catch (Exception e) { imprimirMensagem("Fudeu ao desconectar no input."); }
		try { if (output != null) output.close(); } catch (Exception e) { imprimirMensagem("Fudeu ao desconectar no output."); }
		try { if (socket != null) socket.close(); } catch (Exception e) { imprimirMensagem("Fudeu ao desconectar no socket."); }
	}

	public void run() {
		while(true) {
			try {
				String msg = (String) input.readObject();
				// if console mode print the message and add back the prompt

				System.out.println(msg);
				System.out.print("> ");
			} catch(IOException e) {
				imprimirMensagem("O Servidor fechou a conexão: " + e);
				break;
			} catch(ClassNotFoundException e2) { imprimirMensagem("Fudeu no run...");}
		}
	}

		/**
		 * Main
		 * @param args
		 */
	public static void main(String[] args) {
		int portServidor = 15001;
		String ipServidor = "localhost";
		String userName = "PCharUser";

		switch(args.length) {
			// Caso nenhum argumento seja especificado na linha de comando.
			case 0:
				break;

			case 1:
				ipServidor = args[0];
				break;

			case 2:
				ipServidor = args[0];
				try {
					portServidor = Integer.parseInt(args[1]);
				}
				catch(Exception e) {
					System.out.println("Porta inválida.");
					System.out.println("> Uso: java Cliente [ip do servidor] [porta do servidor] [nome de usuário]");
					return;
				}

			case 3:
				ipServidor = args[0];
				try {
					portServidor = Integer.parseInt(args[1]);
				}
				catch(Exception e) {
					System.out.println("Porta inválida.");
					System.out.println("> Uso: java Cliente [ip do servidor] [porta do servidor] [nome de usuário]");
					return;
				}
				userName = args[2];
				break;

			// invalid number of arguments
			default:
				System.out.println("> Uso: java Cliente [ip do servidor] [porta do servidor] [nome de usuário]");
				return;
		}

		Cliente cliente = new Cliente(ipServidor, portServidor, userName);
		if (!cliente.start()) return;

		// Prompt de Comando do Chat
		Scanner input = new Scanner(System.in);
		while (true) {
			System.out.print("> ");
			String comando = input.nextLine();

			if(comando.equalsIgnoreCase("BYE")) {
				cliente.sendMessage(new Mensagem(Mensagem.LOGOUT, ""));
				// break para desconectar
				break;
			} else if(comando.startsWith("list")) {
				cliente.sendMessage(new Mensagem(Mensagem.LIST, ""));
			} else if(comando.startsWith("msg")) {                            // default to ordinary message
				cliente.sendMessage(new Mensagem(Mensagem.MESSAGE, comando));
			} else if 	(comando.equalsIgnoreCase("HELP")) {
				System.out.println("\nComandos disponíveis:\n" +
				                   "\tLIST - Para ver a lista de usuários conectados.\n" +
				                   "\tMESSAGE - Para enviar mensagem para todos os usuários.\n" +
				                   "\tBYE - Para finalizar a conexão com o servidor.\n");
			} else {
				System.out.println("> Comando \'" + comando + "\' não existe. Use \'help\' para ver os comandos disponíveis.");
			}
		}
		// Desconectando o cliente.
		cliente.desconectar();
		input.close();
	}

	class ListenFromServer extends Thread {
		public void run() {
			while (true) {
				try {
					String msg = (String) input.readObject();
					System.out.println(msg);
					System.out.print("> ");
				} catch (IOException e) {
					imprimirMensagem("O Servidor fechou a conexão: " + e + ".\n");
					break;
				} catch (ClassNotFoundException e2) { imprimirMensagem("Fudeu no List From Server...");}
			}
		}
	}
}
