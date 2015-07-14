package com.pchat;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class Servidor {
	private int port;
	private static int idUnico; // Para identificar a conexão.
	private ArrayList<ClientThread> colecaoClientes;
	private SimpleDateFormat sdf;
	private Boolean aguardandoConexao;

	public Servidor(int port) {
		this.port = port;
		this.sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		this.colecaoClientes = new ArrayList<ClientThread>();
		this.idUnico = 1;
	}

	public void start() {
		aguardandoConexao = true;

		try {
			ServerSocket servidor = new ServerSocket(port);
			System.out.println("Servidor iniciado em " + InetAddress.getLocalHost() + ":" + servidor.getLocalPort() + ".\n");
			imprimirMensagem("Aguardando conexão...");
			while(aguardandoConexao) {

				Socket socket = servidor.accept();

				ClientThread cliente = new ClientThread(socket);
				colecaoClientes.add(cliente);
				cliente.start();
			}

		} catch (IOException e) {
			imprimirMensagem("Erro ao criar Socket: " + e + ".\n");
		}
	}

	public void imprimirMensagem(String mensagem) {
		System.out.println("[" + sdf.format(new Date()) + "] " + mensagem);
	}

	/*
	 *  broadcast
	 */
	public void broadcast(String message) {
		String msg = "[" + sdf.format(new Date()) + "] " + message + "\n";
		System.out.print(msg);

		Iterator<ClientThread> it = colecaoClientes.iterator();
		while(it.hasNext()) {
			ClientThread cliente = it.next();
			if (!cliente.writeMsg(msg)) {
				it.remove();
				imprimirMensagem("Cliente desconectado: " + cliente.username + ".");
			}
		}
	}

	/**
	 * Uma Thread para cada cliente
	 *
	 */
	class ClientThread extends Thread {
		Socket socket;
		ObjectInputStream input;
		ObjectOutputStream output;
		int id;
		String username;
		Mensagem msg;
		String date;
		SimpleDateFormat sdf;

		// Construtor
		ClientThread(Socket socket) {
			id = ++idUnico;
			this.socket = socket;

			try {
				output = new ObjectOutputStream(socket.getOutputStream());
				input = new ObjectInputStream(socket.getInputStream());
				// read the username
				username = (String) input.readObject();
				imprimirMensagem("Usuário conectado: " + username + ".");
			} catch (IOException e) {
				imprimirMensagem("Erro ao criar fluxo de entrada e saída: " + e);
				return;
			} catch (ClassNotFoundException e) { imprimirMensagem("Fudeu no construtor ClientThread..."); }
			date = new Date().toString() + "\n";
		}


		public void run() {
			// A Condição de parada do loop é o LOGOUT.
			aguardandoConexao = true;
			while (aguardandoConexao) {
				try {
					msg = (Mensagem) input.readObject();
				} catch (IOException e) {
					imprimirMensagem(username + " Exception reading Streams: " + e);
					break;
				} catch (ClassNotFoundException e2) {
					imprimirMensagem("Fudeu no run...\n");
					break;
				}

				String message = msg.getMessage();

				switch (msg.getType()) {

					case Mensagem.MESSAGE:
						broadcast(username + ": " + message);
						break;

					case Mensagem.LOGOUT:
						imprimirMensagem("Usuário desconectado: " + username + ".");
						//aguardandoConexao = false;
						break;

					case Mensagem.LIST:
						//try {
						//	writeMsg("Lista de usuários conectados em " + sdf.format(new Date()) + "\n");
						//} catch (Exception e) { imprimirMensagem("Fudeu na Mensagem.LIST");}

						Iterator<ClientThread> it = colecaoClientes.iterator();
						while(it.hasNext()) {
							ClientThread cliente = it.next();
							writeMsg(cliente.username + " desde " + cliente.date);
						}
				}
			}
			// remove  o servidor da lista de clientes
			try {
				colecaoClientes.remove(id);
			} catch (Exception e) {imprimirMensagem("Fudeu na hora de remover pelo id...");}
			close();
		}

		//LOGOUT
		public void remove(int id) {
			Iterator<ClientThread> it = colecaoClientes.iterator();
			while(it.hasNext()) {
				ClientThread cliente = it.next();
				if (cliente.getId() == id) {
					it.remove();
					return;
				}
			}
		}

		// Fechando os streams e a conexão
		private void close() {
			try { if (output != null) output.close(); } catch (Exception e) { }

			try { if (input != null) input.close(); } catch (Exception e) { }

			try { if (socket != null) socket.close(); } catch (Exception e) { }
		}

		/*
		 * Escreve a mensagem para a saída
		 */
		public boolean writeMsg(String msg) {

			if (!socket.isConnected()) {
				close();
				return false;
			}

			try {
				output.writeObject(msg);
			} catch (IOException e) {
				imprimirMensagem("Erro ao enviar mensagem para: " + username);
				//imprimirMensagem(e.toString());
			} catch (NullPointerException ne) {imprimirMensagem("Fudeu na hora de escrever a msg na saida");}

			return true;
		}
	}

	/**
	 * Main
	 * @param args
	 */
    public static void main(String[] args) {
	    int port = 15001; // Porta padrão.

	    switch(args.length) {
		    // Caso nenhum argumento seja especificado na linha de comando.
		    // Nesse caso o servidor vai usar a porta padrão.
		    case 0:
			    break;

		    // Caso a porta seja especificado na linha de comando.
		    case 1:
			    try {
				    port = Integer.parseInt(args[0]);
			    } catch(Exception e) {
				    System.out.println("> Porta inválida.");
				    System.out.println("> Uso: java Servidor [porta]");
				    return;
			    }

			// Para tratar outros casos
		    default:
			    System.out.println("Uso: java Servidor [porta]");
			    return;
	    }

	    // Iniciando o servidor.
	    Servidor servidor = new Servidor(port);
	    servidor.start();
    }
}
